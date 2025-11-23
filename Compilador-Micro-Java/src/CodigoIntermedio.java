/* Alumno: Kevin Ignacio Rojas Duarte
 * NC: 22170801
 * Materia: Lenguajes y automatas 2
 * Profesor: Rosalio Zatarain Cabada
 * Hora: 13:00pm - 14:00pm
 */
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

        // segmento de datos
        for (Simbolo s : ts.getTabla().values()) {
            String id = s.getIdentificador();
            String tipo = s.getTipo();
            asm.append(String.format("%-8s %s      ?\n", id, "boolean".equals(tipo) ? "DB" : "DW"));
        }
        asm.append("\n");
        // segmento de codigo
        asm.append("        .CODE\n");
        asm.append("MAIN    PROC    FAR\n");
        asm.append("        .STARTUP\n");

        // recorrer y traducir statements
        while (idx.get() < tokens.size()) {
            int code = currentCode(tokens, idx);
            if (code == -1) break;
            if (code == 11 /* } */ || code == 10 /* { */ || code == 1 /* class */) {
                idx.incrementAndGet();
                continue;
            }
            translateStatement(tokens, idx, asm, ts, whileCounter);
        }

        // finalización
        asm.append("        .EXIT\n");
        asm.append("MAIN    ENDP\n\n");
        
        // Rutina imprimir_num
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
        int code = currentCode(tokens, idx);
        if (code == -1) return;

        // salta declaraciones: 'int' o 'boolean'
        if (code == 2 /* int */ || code == 3 /* boolean */) {
            for (int k = 0; k < 3 && idx.get() < tokens.size(); k++) {
                idx.incrementAndGet();
            }
            return;
        }

        if (code == 5 /* print */) {
            // print ( Expression ) ;
            idx.incrementAndGet(); // consume 'print'
            idx.incrementAndGet();  // consume '('
            generateExpressionIntoAX(tokens, idx, asm, ts);
           idx.incrementAndGet(); // consume ')'
            idx.incrementAndGet();// consume ';'
            // Llamada imprimir
            asm.append("        CALL    IMPRIMIR_NUM\n");
            asm.append("        MOV     DL, 0AH\n");
            asm.append("        MOV     AH, 02H\n");
            asm.append("        INT     21H\n");
            asm.append("        MOV     DL, 0DH\n");
            asm.append("        INT     21H\n");
            return;
        }

        if (code == 4 /* while */) {
            int thisWhile = whileCounter.getAndIncrement();
            String lblWhile = "WHILE" + thisWhile;
            String lblEnd = "ENDW" + thisWhile;
            idx.incrementAndGet(); // consume 'while'
            idx.incrementAndGet(); // consume '('

            //ID/NUM/op/ID/NUM
            Token leftTok = safeGet(tokens, idx.get());
            idx.incrementAndGet();
            Token opTok = safeGet(tokens, idx.get());
            idx.incrementAndGet();
            Token rightTok = safeGet(tokens, idx.get());
            idx.incrementAndGet();
            idx.incrementAndGet();  // consume ')'

            String left = (leftTok != null) ? leftTok.getToken() : "0";
            int opCode = (opTok != null) ? opTok.getCodigo() : -1;
            String right = (rightTok != null) ? rightTok.getToken() : "0";

            // etiqueta inicio
            asm.append(lblWhile).append(":\n");
            // cargar left en AX (si es literal o id)
            loadOperandIntoAX(left, asm, ts);
            // comparar
            asm.append("        CMP     AX, ").append(right).append("\n");
            // saltar a fin según operación
            if (opCode == 19 /* > */) {
                asm.append("        JLE     ").append(lblEnd).append("\n");
            } else if (opCode == 18 /* < */) {
                asm.append("        JGE     ").append(lblEnd).append("\n");
            } else {
                asm.append("        JMP     ").append(lblEnd).append("\n");
            }

            idx.incrementAndGet();

            // traducir statements hasta  '}'
            while (idx.get() < tokens.size()) {
                int cur = currentCode(tokens, idx);
                if (cur == 11 /* } */) {
                    idx.incrementAndGet();
                    break;
                }
                translateStatement(tokens, idx, asm, ts, whileCounter);
            }

            // salto al inicio y etiqueta fin
            asm.append("        JMP     ").append(lblWhile).append("\n");
            asm.append(lblEnd).append(":\n");
            return;
        }

        // asignacion: Identifier = Expression ;
        if (code == 9 /* ID */) {
            String id = currentLexeme(tokens, idx);
            idx.incrementAndGet(); // consume id
            idx.incrementAndGet(); //consume '='

            int exprStart = idx.get();
            int scan = exprStart;
            while (scan < tokens.size() && tokens.get(scan).getCodigo() != 14 /* ; */) scan++;
            int exprEnd = scan; 

            if (exprEnd - exprStart == 1) {
                Token r = tokens.get(exprStart);
                int rcode = r.getCodigo();
                String rlex = r.getToken();
                // mover literal o id directamente
                if (rcode == 6 /* true */) {
                    // boolean true -> 1
                    if (ts.getSimbolo(id) != null && "boolean".equals(ts.getSimbolo(id).getTipo())) {
                        asm.append("        MOV     ").append(id).append(", 1\n");
                    } else {
                        asm.append("        MOV     ").append(id).append(", 1\n");
                    }
                } else if (rcode == 7 /* false */) {
                    asm.append("        MOV     ").append(id).append(", 0\n");
                } else if (rcode == 8 /* NUM */) {
                    asm.append("        MOV     ").append(id).append(", ").append(rlex).append("\n");
                } else { // ID
                    Simbolo dest = ts.getSimbolo(id);
                    if (dest != null && "boolean".equals(dest.getTipo())) {
                        asm.append("        MOV     ").append(id).append(", ").append(rlex).append("\n");
                    } else {
                        asm.append("        MOV     ").append(id).append(", ").append(rlex).append("\n");
                    }
                }
                idx.set(exprEnd + 1);
                return;
            }

            // generar expresión en AX y mover según tipo destino
            idx.set(exprStart);
            generateExpressionIntoAX(tokens, idx, asm, ts);
            idx.incrementAndGet();// consumir ';'

            Simbolo destSym = ts.getSimbolo(id);
            String destType = (destSym != null) ? destSym.getTipo() : "int";
            if ("boolean".equals(destType)) {
                asm.append("        MOV     ").append(id).append(", AL\n");
            } else {
                asm.append("        MOV     ").append(id).append(", AX\n");
            }
            return;
        }
        idx.incrementAndGet();
    }

    // Genera código que deja el valor de la expresión en AX.
    private static void generateExpressionIntoAX(ArrayList<Token> tokens, AtomicInteger idx,
                                                 StringBuilder asm, tablaSimbolos ts) {
        if (idx.get() >= tokens.size()) return;
        Token t1 = tokens.get(idx.get());
        if (t1 == null) return;
        int code1 = t1.getCodigo();
        String s1 = t1.getToken();
        if (s1 == null) return;
        if (code1 == 14 /* ; */ || code1 == 13 /* ) */ || code1 == 10 /* { */ || code1 == 11 /* } */) return;

        if (idx.get() + 1 < tokens.size()) {
            int possibleOpCode = tokens.get(idx.get() + 1).getCodigo();
            if (possibleOpCode == 16 /* + */ || possibleOpCode == 17 /* - */ || possibleOpCode == 20 /* * */
                || possibleOpCode == 18 /* < */ || possibleOpCode == 19 /* > */) {
    
                String left = s1;
                int op = possibleOpCode;
                idx.incrementAndGet(); 
                idx.incrementAndGet();
                if (idx.get() >= tokens.size()) return;
                Token rightTok = tokens.get(idx.get());
                if (rightTok == null) return;
                String right = rightTok.getToken();
                int rightCode = rightTok.getCodigo();
                idx.incrementAndGet();

                // comparaciones <, >
                if (op == 18 /* < */ || op == 19 /* > */) {
                    loadOperandIntoAX(left, asm, ts);
                    asm.append("        CMP     AX, ").append(right).append("\n");
                    String lblTrue = newLabel("CMP_TRUE");
                    String lblEnd = newLabel("CMP_END");
                    if (op == 19 /* > */) {
                        asm.append("        JG      ").append(lblTrue).append("\n");
                    } else {
                        asm.append("        JL      ").append(lblTrue).append("\n");
                    }
                    asm.append("        MOV     AX, 0\n");
                    asm.append("        JMP     ").append(lblEnd).append("\n");
                    asm.append(lblTrue).append(":\n");
                    asm.append("        MOV     AX, 1\n");
                    asm.append(lblEnd).append(":\n");
                    return;
                }

                // operaciones aritméticas +, -, *
                loadOperandIntoAX(left, asm, ts);
                if (op == 16 /* + */) {
                    asm.append("        ADD     AX, ").append(right).append("\n");
                } else if (op == 17 /* - */) {
                    asm.append("        SUB     AX, ").append(right).append("\n");
                } else if (op == 20 /* * */) {
                    asm.append("        MOV     BX, ").append(right).append("\n");
                    asm.append("        MUL    BX\n");
                }
                return;
            }
        }

        idx.incrementAndGet();
        if (code1 == 6 /* true */) {
            asm.append("        MOV     AX, 1\n");
            return;
        }
        if (code1 == 7 /* false */) {
            asm.append("        MOV     AX, 0\n");
            return;
        }
        if (code1 == 8 /* NUM */) {
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

    // Carga un operando 
    private static void loadOperandIntoAX(String operand, StringBuilder asm, tablaSimbolos ts) {
        if (operand == null) return;
        if (operand.equals(";") || operand.equals(")") || operand.equals("{") || operand.equals("}")) return;
        if (operand.matches("\\d+")) {
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

    private static int currentCode(ArrayList<Token> tokens, AtomicInteger idx) {
        if (idx.get() >= tokens.size()) return -1;
        return tokens.get(idx.get()).getCodigo();
    }

    private static String currentLexeme(ArrayList<Token> tokens, AtomicInteger idx) {
        if (idx.get() >= tokens.size()) return null;
        Token t = tokens.get(idx.get());
        return (t == null) ? null : t.getToken();
    }

    private static Token safeGet(ArrayList<Token> tokens, int pos) {
        if (pos < 0 || pos >= tokens.size()) return null;
        return tokens.get(pos);
    }

    // Contador para etiquetas auxiliares de comparaciones
    private static AtomicInteger auxLabelCounter = new AtomicInteger(0);
    private static String newLabel(String base) {
        int n = auxLabelCounter.getAndIncrement();
        return base + n;
    }
}
