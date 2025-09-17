import java.util.ArrayList;
import java.util.HashMap;

public class Semantico {
    private HashMap<String, String> tablaSimbolos;
    private ArrayList<String> errores;

    public Semantico() {
        tablaSimbolos = new HashMap<>();
        errores = new ArrayList<>();
    }

    public void analizar(ArrayList<Token> tokens) {
        tablaSimbolos.clear();
        errores.clear();
        tokens.remove(0) ; // Remover 'class'
        tokens.remove(0) ; // Remover ID (nombre de la clase)
        tokens.remove(0) ; // Remover '{'
        tokens.remove(tokens.size() - 1) ; // Remover '}'
        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);

            // Declaración de variable: int o boolean seguido de ID y ;
            if (t.getCodigo() == 2 || t.getCodigo() == 3) { // int=2, boolean=3
                String tipo = (t.getCodigo() == 2) ? "int" : "boolean";
                if (i + 1 < tokens.size() && tokens.get(i + 1).getCodigo() == 9) { // ID=9
                    String id = tokens.get(i + 1).getToken();
                    if (tablaSimbolos.containsKey(id)) {
                        errores.add("Error: Variable '" + id + "' ya declarada.");
                    } else {
                        tablaSimbolos.put(id, tipo);
                    }
                }
            }

            // Asignación: ID = expresión ;
            if (t.getCodigo() == 9) { // ID
                String id = t.getToken();
                if (i + 1 < tokens.size() && tokens.get(i + 1).getCodigo() == 15) { // = = 15
                    if (!tablaSimbolos.containsKey(id)) {
                        errores.add("Error: Variable '" + id + "' no declarada antes de asignar valor.");
                        continue;
                    }
                    String tipoVar = tablaSimbolos.get(id);
                    String tipoExpr = evaluarExpresion(tokens, i + 2, tipoVar);
                    if (tipoExpr == null) {
                        errores.add("Error: Expresión inválida asignada a '" + id + "'.");
                    } else if (!tipoVar.equals(tipoExpr)) {
                        errores.add("Error: Variable '" + id + "' es " + tipoVar + " y no puede recibir un " + tipoExpr + ".");
                    }
                }
            }
        }
    }

    /**
     * Evalúa el tipo resultante de una expresión simple (ID, NUM, true/false o combinaciones con +, -, *, <, >)
     */
    private String evaluarExpresion(ArrayList<Token> tokens, int indice, String tipoEsperado) {
        if (indice >= tokens.size()) return null;
        Token primero = tokens.get(indice);

        // Identificador
        if (primero.getCodigo() == 9) { // ID
            String id = primero.getToken();
            if (!tablaSimbolos.containsKey(id)) {
                errores.add("Error: Variable '" + id + "' usada sin declarar.");
                return null;
            }
            String tipo = tablaSimbolos.get(id);

            // Operador binario
            if (indice + 1 < tokens.size()) {
                int op = tokens.get(indice + 1).getCodigo();
                if (op == 16 || op == 17 || op == 20) { // + - *
                    String tipoDerecho = evaluarExpresion(tokens, indice + 2, tipoEsperado);
                    if (tipoDerecho == null) return null;
                    if (!tipo.equals("int") || !tipoDerecho.equals("int")) {
                        errores.add("Error: Operación aritmética requiere enteros pero se encontraron " + tipo + " y " + tipoDerecho + ".");
                        return null;
                    }
                    return "int";
                }
                if (op == 18 || op == 19) { // < >
                    String tipoDerecho = evaluarExpresion(tokens, indice + 2, tipoEsperado);
                    if (tipoDerecho == null) return null;
                    if (!tipo.equals("int") || !tipoDerecho.equals("int")) {
                        errores.add("Error: Comparación requiere enteros pero se encontraron " + tipo + " y " + tipoDerecho + ".");
                        return null;
                    }
                    return "boolean";
                }
            }
            return tipo;
        }

        // Número
        if (primero.getCodigo() == 8) { // NUM
            return "int";
        }

        // Booleanos
        if (primero.getCodigo() == 6 || primero.getCodigo() == 7) { // true=6, false=7
            return "boolean";
        }

        return null;
    }

    public ArrayList<String> getErrores() {
        return errores;
    }

    public HashMap<String, String> getTablaSimbolos() {
        return tablaSimbolos;
    }
}