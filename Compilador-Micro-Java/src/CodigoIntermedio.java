import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
public class CodigoIntermedio {

    public static String generar(ArrayList<Token> tokens, tablaSimbolos ts) {
        StringBuilder asm = new StringBuilder();
        AtomicInteger idx = new AtomicInteger(0);
        AtomicInteger whileCounter = new AtomicInteger(0);

        // Cabecera
        asm.append("TITLE   MicroJava\n");
        asm.append("        .MODEL  Small\n");
        asm.append("        .STACK  100h\n");
        asm.append("        .DATA\n");

        // Sección DATA: declarar símbolos según tablaSimbolos
        for (Simbolo s : ts.getTabla().values()) {
            String id = s.getIdentificador();
            String tipo = s.getTipo();
            if ("int".equals(tipo)) {
                asm.append(String.format("%-8s DW      ?\n", id));
            } else if ("boolean".equals(tipo)) {
                asm.append(String.format("%-8s DB      ?\n", id));
            } else {
                // por defecto DW
                asm.append(String.format("%-8s DW      ?\n", id));
            }
        }
        asm.append("\n");
        // Código
        asm.append("        .CODE\n");
        asm.append("MAIN    PROC    FAR\n");
        asm.append("        .STARTUP\n");

        // recorrer y traducir statements
        while (idx.get() < tokens.size()) {
            String tk = current(tokens, idx);
            if (tk == null) break;
            // Skip possible class/brace tokens if present
            if (tk.equals("}") || tk.equals("{") || tk.equals("class") ) {
                idx.incrementAndGet();
                continue;
            }
            translateStatement(tokens, idx, asm, ts, whileCounter);
        }

        // Epílogo
        asm.append("        .EXIT\n");
        asm.append("MAIN    ENDP\n\n");

        // Rutina imprimir_num (igual que ejemplo). Asume AX contiene el número a imprimir.
        asm.append("IMPRIMIR_NUM PROC\n");
        asm.append("        MOV     BX, 10\n");
        asm.append("        XOR     CX, CX\n\n");
        asm.append("DIV_LOOP:\n");
        asm.append("        XOR     DX, DX\n");
        asm.append("        DIV     BX\n");
        asm.append("        PUSH    DX\n");
        asm.append("        INC     CX\n");
        asm.append("        CMP     AX, 0\n");
        asm.append("        JNE     DIV_LOOP\n\n");
        asm.append("IMPRESION_LOOP:\n");
        asm.append("        POP     DX\n");
        asm.append("        ADD     DL, '0'\n");
        asm.append("        MOV     AH, 02H\n");
        asm.append("        INT     21H\n");
        asm.append("        LOOP    IMPRESION_LOOP\n");
        asm.append("        RET\n");
        asm.append("IMPRIMIR_NUM ENDP\n");
        asm.append("        END     MAIN\n");

        return asm.toString();
    }

    // Traduce un statement empezando en idx; avanza idx apropiadamente
    private static void translateStatement(ArrayList<Token> tokens, AtomicInteger idx,
                                           StringBuilder asm, tablaSimbolos ts,
                                           AtomicInteger whileCounter) {
        String tk = current(tokens, idx);
        if (tk == null) return;

        // SKIP declarations: 'int' or 'boolean' lines (consume until ';')
        if ("int".equals(tk) || "boolean".equals(tk)) {
            idx.incrementAndGet(); // consume type
            // skip identifiers, possible initializers, until ';'
            while (idx.get() < tokens.size() && !";".equals(current(tokens, idx))) {
                idx.incrementAndGet();
            }
            if (idx.get() < tokens.size() && ";".equals(current(tokens, idx))) idx.incrementAndGet();
            return;
        }

        if ("print".equals(tk)) {
            // print ( Expression ) ;
            idx.incrementAndGet(); // consume 'print'
            expect(tokens, idx, "(");
            // colocar resultado de la expresión en AX
            generateExpressionIntoAX(tokens, idx, asm, ts);
            expect(tokens, idx, ")");
            expect(tokens, idx, ";");
            // Llamada imprimir
            asm.append("        CALL    IMPRIMIR_NUM\n");
            asm.append("        MOV     DL, 0AH\n");
            asm.append("        MOV     AH, 02H\n");
            asm.append("        INT     21H\n");
            asm.append("        MOV     DL, 0DH\n");
            asm.append("        INT     21H\n");
            return;
        }

        if ("while".equals(tk)) {
            // while ( Expression ) { Statement* }
            int thisWhile = whileCounter.getAndIncrement();
            String lblWhile = "WHILE" + thisWhile;
            String lblEnd = "ENDW" + thisWhile;
            idx.incrementAndGet(); // consume 'while'
            expect(tokens, idx, "(");

            // parse condition expression for > or <
            Token leftTok = tokens.get(idx.get());
            String left = leftTok.getToken();
            idx.incrementAndGet();
            Token opTok = tokens.get(idx.get());
            String op = opTok.getToken();
            idx.incrementAndGet();
            Token rightTok = tokens.get(idx.get());
            String right = rightTok.getToken();
            idx.incrementAndGet();
            expect(tokens, idx, ")");

            // etiqueta inicio
            asm.append(lblWhile).append(":\n");
            // cargar left en AX (si es literal o id)
            loadOperandIntoAX(left, asm, ts);
            // comparar
            asm.append("        CMP     AX, ").append(right).append("\n");
            // saltar a fin según operación
            if (">".equals(op)) {
                asm.append("        JLE     ").append(lblEnd).append("\n");
            } else if ("<".equals(op)) {
                asm.append("        JGE     ").append(lblEnd).append("\n");
            }

            // consumir '{'
            expect(tokens, idx, "{");

            // traducir statements hasta matching '}'
            while (idx.get() < tokens.size()) {
                String cur = current(tokens, idx);
                if ("}".equals(cur)) {
                    idx.incrementAndGet(); // consume '}'
                    break;
                }
                translateStatement(tokens, idx, asm, ts, whileCounter);
            }

            // salto al inicio y etiqueta fin
            asm.append("        JMP     ").append(lblWhile).append("\n");
            asm.append(lblEnd).append(":\n");
            return;
        }

        // Assignment: Identifier = Expression ;
        if (isIdentifier(tk)) {
            String id = tk;
            idx.incrementAndGet(); // consume id
            expect(tokens, idx, "=");

            // detect simple RHS (until semicolon)
            int exprStart = idx.get();
            int scan = exprStart;
            while (scan < tokens.size() && !";".equals(tokens.get(scan).getToken())) scan++;
            int exprEnd = scan; // position of ';'
            List<String> exprTokens = new ArrayList<>();
            for (int i = exprStart; i < exprEnd; i++) exprTokens.add(tokens.get(i).getToken());

            Simbolo destSym = ts.getSimbolo(id);
            String destType = (destSym != null) ? destSym.getTipo() : "int";

            boolean handledDirect = false;
            // simple single token RHS
            if (exprTokens.size() == 1) {
                String right = exprTokens.get(0);
                if ("true".equals(right) || "false".equals(right) || isNumeric(right)) {
                    // literal assignment -> MOV <id>, imm
                    String imm = ("true".equals(right)) ? "1" : ("false".equals(right) ? "0" : right);
                    if ("int".equals(destType)) {
                        asm.append("        MOV     ").append(id).append(", ").append(imm).append("\n");
                    } else if ("boolean".equals(destType)) {
                        asm.append("        MOV     ").append(id).append(", ").append(imm).append("\n");
                    } else {
                        asm.append("        MOV     ").append(id).append(", ").append(imm).append("\n");
                    }
                    handledDirect = true;
                } else if (isIdentifier(right)) {
                    // simple identifier -> move memory-to-memory with proper handling of sizes
                    Simbolo srcSym = ts.getSimbolo(right);
                    String srcType = (srcSym != null) ? srcSym.getTipo() : "int";
                    if ("int".equals(destType) && "int".equals(srcType)) {
                        asm.append("        MOV     ").append(id).append(", ").append(right).append("\n");
                    } else if ("boolean".equals(destType) && "boolean".equals(srcType)) {
                        asm.append("        MOV     ").append(id).append(", ").append(right).append("\n");
                    } else if ("int".equals(destType) && "boolean".equals(srcType)) {
                        // zero-extend byte -> word
                        asm.append("        MOV     AL, ").append(right).append("\n");
                        asm.append("        MOV     AH, 0\n");
                        asm.append("        MOV     ").append(id).append(", AX\n");
                    } else if ("boolean".equals(destType) && "int".equals(srcType)) {
                        // truncate word -> byte (take low byte)
                        asm.append("        MOV     AX, ").append(right).append("\n");
                        asm.append("        MOV     ").append(id).append(", AL\n");
                    } else {
                        // fallback: load into AX then store
                        // prepare idx to point to RHS for generateExpressionIntoAX
                        idx.set(exprStart);
                        generateExpressionIntoAX(tokens, idx, asm, ts);
                        if ("int".equals(destType)) {
                            asm.append("        MOV     ").append(id).append(", AX\n");
                        } else {
                            asm.append("        MOV     ").append(id).append(", AL\n");
                        }
                    }
                    handledDirect = true;
                }
            }

            if (handledDirect) {
                // advance idx to after semicolon
                idx.set(exprEnd + 1);
                return;
            }

            // fallback: generate expression into AX and then move to variable respecting sizes
            idx.set(exprStart);
            generateExpressionIntoAX(tokens, idx, asm, ts);
            expect(tokens, idx, ";");
            if ("int".equals(destType)) {
                asm.append("        MOV     ").append(id).append(", AX\n");
            } else if ("boolean".equals(destType)) {
                asm.append("        MOV     ").append(id).append(", AL\n");
            } else {
                asm.append("        MOV     ").append(id).append(", AX\n");
            }
            return;
        }

        // If unknown token, just consume it to avoid infinite loop
        idx.incrementAndGet();
    }

    // Genera código que deja el valor de la expresión en AX. Avanza idx.
    private static void generateExpressionIntoAX(ArrayList<Token> tokens, AtomicInteger idx,
                                                 StringBuilder asm, tablaSimbolos ts) {
        if (idx.get() >= tokens.size()) return;
        Token t1 = tokens.get(idx.get());
        if (t1 == null) return;
        String s1 = t1.getToken();
        if (s1 == null) return;
        if (s1.equals(";") || s1.equals(")") || s1.equals("{") || s1.equals("}")) return;

        // lookahead for binary operator
        if (idx.get() + 1 < tokens.size()) {
            String possibleOp = tokens.get(idx.get() + 1).getToken();
            if ("+".equals(possibleOp) || "-".equals(possibleOp) || "*".equals(possibleOp)
                || "<".equals(possibleOp) || ">".equals(possibleOp)) {
                // binary
                String left = s1;
                String op = possibleOp;
                // advance to right operand
                idx.incrementAndGet();
                idx.incrementAndGet();
                if (idx.get() >= tokens.size()) return;
                Token rightTok = tokens.get(idx.get());
                if (rightTok == null) return;
                String right = rightTok.getToken();
                idx.incrementAndGet();
                // comparison (<,>)
                if ("<".equals(op) || ">".equals(op)) {
                    loadOperandIntoAX(left, asm, ts);
                    asm.append("        CMP     AX, ").append(right).append("\n");
                    String lblTrue = newLabel("CMP_TRUE");
                    String lblEnd = newLabel("CMP_END");
                    asm.append("        J"); // choose below
                    if (">".equals(op)) {
                        asm.append("G      ").append(lblTrue).append("\n");
                    } else {
                        asm.append("L      ").append(lblTrue).append("\n");
                    }
                    // false
                    asm.append("        MOV     AX, 0\n");
                    asm.append("        JMP     ").append(lblEnd).append("\n");
                    // true
                    asm.append(lblTrue).append(":\n");
                    asm.append("        MOV     AX, 1\n");
                    asm.append(lblEnd).append(":\n");
                    return;
                }

                // arithmetic (+,-,*)
                loadOperandIntoAX(left, asm, ts);
                if ("+".equals(op)) {
                    if (isNumeric(right)) {
                        asm.append("        ADD     AX, ").append(right).append("\n");
                    } else {
                        // right is identifier: ensure word load
                        Simbolo s = ts.getSimbolo(right);
                        if (s != null && "boolean".equals(s.getTipo())) {
                            // add byte -> extend to BX first
                            asm.append("        MOV     BL, ").append(right).append("\n");
                            asm.append("        MOV     BH, 0\n");
                            asm.append("        ADD     AX, BX\n");
                        } else {
                            asm.append("        ADD     AX, ").append(right).append("\n");
                        }
                    }
                } else if ("-".equals(op)) {
                    if (isNumeric(right)) {
                        asm.append("        SUB     AX, ").append(right).append("\n");
                    } else {
                        Simbolo s = ts.getSimbolo(right);
                        if (s != null && "boolean".equals(s.getTipo())) {
                            asm.append("        MOV     BL, ").append(right).append("\n");
                            asm.append("        MOV     BH, 0\n");
                            asm.append("        SUB     AX, BX\n");
                        } else {
                            asm.append("        SUB     AX, ").append(right).append("\n");
                        }
                    }
                } else if ("*".equals(op)) {
                    if (isNumeric(right)) {
                        asm.append("        MOV     BX, ").append(right).append("\n");
                    } else {
                        asm.append("        MOV     BX, ").append(right).append("\n");
                    }
                    asm.append("        IMUL    BX\n"); // AX = AX * BX
                }
                return;
            }
        }

        // Not binary: single token
        idx.incrementAndGet();
        if ("true".equals(s1)) {
            asm.append("        MOV     AX, 1\n");
            return;
        }
        if ("false".equals(s1)) {
            asm.append("        MOV     AX, 0\n");
            return;
        }
        if (isNumeric(s1)) {
            asm.append("        MOV     AX, ").append(s1).append("\n");
            return;
        }
        // identifier
        Simbolo sym = ts.getSimbolo(s1);
        if (sym != null && "boolean".equals(sym.getTipo())) {
            asm.append("        MOV     AL, ").append(s1).append("\n");
            asm.append("        MOV     AH, 0\n");
        } else {
            asm.append("        MOV     AX, ").append(s1).append("\n");
        }
    }

    // Carga un operando (literal o id) en AX (si id es DB, pone AL y limpia AH)
    private static void loadOperandIntoAX(String operand, StringBuilder asm, tablaSimbolos ts) {
        if (operand == null) return;
        if (operand.equals(";") || operand.equals(")") || operand.equals("{") || operand.equals("}")) return;
        if (isNumeric(operand)) {
            asm.append("        MOV     AX, ").append(operand).append("\n");
            return;
        }
        Simbolo s = ts.getSimbolo(operand);
        if (s != null && "boolean".equals(s.getTipo())) {
            asm.append("        MOV     AL, ").append(operand).append("\n");
            asm.append("        MOV     AH, 0\n");
        } else {
            asm.append("        MOV     AX, ").append(operand).append("\n");
        }
    }

    // helpers ----------------------------------------------------------------

    private static String currentTokenString(ArrayList<Token> tokens, AtomicInteger idx) {
        if (idx.get() >= tokens.size()) return null;
        return tokens.get(idx.get()).getToken();
    }

    private static String current(ArrayList<Token> tokens, AtomicInteger idx) {
        if (idx.get() >= tokens.size()) return null;
        Token t = tokens.get(idx.get());
        return (t == null) ? null : t.getToken();
    }

    private static void expect(ArrayList<Token> tokens, AtomicInteger idx, String expected) {
        if (idx.get() < tokens.size() && expected.equals(current(tokens, idx))) {
            idx.incrementAndGet();
        } else {
            // intentar avanzar para evitar bucle infinito
            if (idx.get() < tokens.size()) idx.incrementAndGet();
        }
    }

    private static boolean isNumeric(String s) {
        return s != null && s.matches("\\d+");
    }

    private static boolean isIdentifier(String s) {
        return s != null && s.matches("[A-Za-z_][A-Za-z0-9_]*");
    }

    // Contador para etiquetas auxiliares de comparaciones
    private static AtomicInteger auxLabelCounter = new AtomicInteger(0);
    private static String newLabel(String base) {
        int n = auxLabelCounter.getAndIncrement();
        return base + n; // devuelve nombre sin ':'; llamador agrega ':' si quiere
    }
}
