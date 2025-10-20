/* Alumno: Kevin Ignacio Rojas Duarte
 * NC: 22170801
 * Materia: Lenguajes y automatas 2
 * Profesor: Rosalio Zatarain Cabada
 * Hora: 13:00pm - 14:00pm
 */
import java.util.*;
import java.util.regex.*;
public class PruebaCodigoIntermedio {

    public static String traducirACodigoIntermedio(String codigoFuente) {
        StringBuilder asm = new StringBuilder();
        List<String> lineas = Arrays.asList(codigoFuente.split("\\n"));
        int whileCount = 0;
        Stack<Integer> whileStack = new Stack<>();
        for (int i = 0; i < lineas.size(); i++) {
            String linea = lineas.get(i).trim();

            // Asignación simple o con expresión
            Matcher mAsig = Pattern.compile("(\\w+)\\s*=\\s*(.+);").matcher(linea);
            if (mAsig.matches()) {
                String id = mAsig.group(1);
                String expr = mAsig.group(2).trim();
                // Expresión binaria
                Matcher mExpr = Pattern.compile("(\\w+|\\d+)\\s*([+\\-*>|<])\\s*(\\w+|\\d+)").matcher(expr);
                if (mExpr.matches()) {
                    String op1 = mExpr.group(1);
                    String op = mExpr.group(2);
                    String op2 = mExpr.group(3);

                    asm.append("MOV AX, ").append(isNumeric(op1) ? op1 : op1).append("\n");
                    switch (op) {
                        case "+": asm.append("ADD AX, ").append(isNumeric(op2) ? op2 : op2).append("\n"); break;
                        case "-": asm.append("SUB AX, ").append(isNumeric(op2) ? op2 : op2).append("\n"); break;
                        case "*": asm.append("MOV BX, ").append(isNumeric(op2) ? op2 : op2).append("\n");
                                  asm.append("MUL BX\n"); break;
                    }
                    asm.append("MOV ").append(id).append(", AX\n\n");
                    continue;
                }
                // Asignación directa
                asm.append("MOV AX, ").append(isNumeric(expr) ? expr : expr).append("\n");
                asm.append("MOV ").append(id).append(", AX\n\n");
                continue;
            }

            // print
            Matcher mPrint = Pattern.compile("print\\((\\w+)\\);").matcher(linea);
            if (mPrint.matches()) {
                String id = mPrint.group(1);
                asm.append("MOV DX, ").append(id).append("\n");
                asm.append("CALL PRINT_NUM\n\n");
                continue;
            }

            // while
            Matcher mWhile = Pattern.compile("while\\s*\\((\\w+)\\s*([><])\\s*(\\w+|\\d+)\\)\\s*\\{").matcher(linea);
            if (mWhile.matches()) {
                String op1 = mWhile.group(1);
                String op = mWhile.group(2);
                String op2 = mWhile.group(3);
                String etiqueta = "WHILE_LOOP" + whileCount;
                String etiquetaFin = "END_WHILE" + whileCount;
                whileStack.push(whileCount);
                whileCount++;

                asm.append(etiqueta).append(":\n");
                asm.append("MOV AX, ").append(op1).append("\n");
                asm.append("CMP AX, ").append(isNumeric(op2) ? op2 : op2).append("\n");
                if (op.equals(">")) {
                    asm.append("JLE ").append(etiquetaFin).append("\n");
                } else if (op.equals("<")) {
                    asm.append("JGE ").append(etiquetaFin).append("\n");
                }
                continue;
            }

            // fin de while
            if (linea.equals("}")) {
                int num = whileStack.pop();
                asm.append("JMP WHILE_LOOP").append(num).append("\n");
                asm.append("END_WHILE").append(num).append(":\n\n");
                continue;
            }
        }

        return asm.toString();
    }
    private static boolean isNumeric(String str) {
        return str.matches("\\d+");
    }
}
