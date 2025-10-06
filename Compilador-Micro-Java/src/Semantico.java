import java.util.ArrayList;

public class Semantico {
    private tablaSimbolos tablaSimbolos;
    private StringBuilder errores;
    private int contadorMemoria;

    public Semantico() {
        tablaSimbolos = new tablaSimbolos();
        errores = new StringBuilder();
        contadorMemoria = 0;
    }

    public void analizar(ArrayList<Token> tokens) {
        tablaSimbolos = new tablaSimbolos();
        errores.setLength(0);
        contadorMemoria = 0;
        tokens.remove(0); // Remover 'class'
        tokens.remove(0); // Remover ID (nombre de la clase)
        tokens.remove(0); // Remover '{'
        tokens.remove(tokens.size() - 1); // Remover '}'
        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);

            // Declaración de variable: int o boolean seguido de ID y ;
            if (t.getCodigo() == 2 || t.getCodigo() == 3) { // int=2, boolean=3
                String tipo = (t.getCodigo() == 2) ? "int" : "boolean";
                int bytes = (tipo.equals("int")) ? 2 : 1;
                if (i + 1 < tokens.size() && tokens.get(i + 1).getCodigo() == 9) { // ID=9
                    String id = tokens.get(i + 1).getToken();
                    if (tablaSimbolos.existe(id)) {
                        errores.append("Error: Variable '").append(id).append("' ya declarada.\n");
                    } else {
                        Simbolo simbolo = new Simbolo(id, tipo, null, contadorMemoria);
                        tablaSimbolos.agregarSimbolo(simbolo);
                        contadorMemoria += bytes;
                    }
                }
            }

            // Asignación: ID = expresión ;
            if (t.getCodigo() == 9) { // ID
                String id = t.getToken();
                if (i + 1 < tokens.size() && tokens.get(i + 1).getCodigo() == 15) { // = = 15
                    if (!tablaSimbolos.existe(id)) {
                        errores.append("Error: Variable '").append(id).append("' no declarada antes de asignar valor.\n");
                        continue;
                    }
                    Simbolo simbolo = tablaSimbolos.getSimbolo(id);
                    String tipoVar = simbolo.getTipo();
                    ResultadoExpr resultado = evaluarExpresion(tokens, i + 2, tipoVar);
                    if (resultado.tipo == null) {
                        errores.append("Error: Expresión inválida asignada a '").append(id).append("'.\n");
                        simbolo.setValor(null);
                    } else if (!tipoVar.equals(resultado.tipo)) {
                        errores.append("Error: Variable '").append(id).append("' es ").append(tipoVar)
                               .append(" y no puede recibir un ").append(resultado.tipo).append(".\n");
                        simbolo.setValor(null);
                    } else {
                        // Solo asigna valor si es un literal (NUM, true, false), si no, conserva el valor actual
                        if (resultado.valorLiteral) {
                            simbolo.setValor(resultado.valor);
                        }
                        // Si no es literal, NO modificar el valor actual
                    }
                }
            }
        }
        System.out.println(tablaSimbolos.toString());
    }

    // Clase auxiliar para devolver tipo y si es literal
    private static class ResultadoExpr {
        String tipo;
        String valor;
        boolean valorLiteral; // true si es NUM, true, false; false si es variable o expresión
        ResultadoExpr(String tipo, String valor, boolean valorLiteral) {
            this.tipo = tipo;
            this.valor = valor;
            this.valorLiteral = valorLiteral;
        }
    }

    /**
     * Evalúa el tipo de una expresión simple (ID, NUM, true/false o combinaciones con +, -, *, <, >)
     */
    private ResultadoExpr evaluarExpresion(ArrayList<Token> tokens, int indice, String tipoEsperado) {
        Token primero = tokens.get(indice);

        // Identificador
        if (primero.getCodigo() == 9) { // ID
            String id = primero.getToken();
            if (!tablaSimbolos.existe(id)) {
                errores.append("Error: Variable '").append(id).append("' usada sin declarar.\n");
                return new ResultadoExpr(null, null, false);
            }
            Simbolo simbolo = tablaSimbolos.getSimbolo(id);
            String tipo = simbolo.getTipo();

            // Operador binario
            if (indice + 1 < tokens.size()) {
                int op = tokens.get(indice + 1).getCodigo();
                if (op == 16 || op == 17 || op == 20) { // + - *
                    ResultadoExpr derecho = evaluarExpresion(tokens, indice + 2, tipoEsperado);
                    if (derecho.tipo == null) return new ResultadoExpr(null, null, false);
                    if (!tipo.equals("int") || !derecho.tipo.equals("int")) {
                        errores.append("Error: Operación aritmética requiere enteros pero se encontraron ")
                               .append(tipo).append(" y ").append(derecho.tipo).append(".\n");
                        return new ResultadoExpr(null, null, false);
                    }
                    return new ResultadoExpr("int", null, false);
                }
                if (op == 18 || op == 19) { // < >
                    ResultadoExpr derecho = evaluarExpresion(tokens, indice + 2, tipoEsperado);
                    if (derecho.tipo == null) return new ResultadoExpr(null, null, false);
                    if (!tipo.equals("int") || !derecho.tipo.equals("int")) {
                        errores.append("Error: Comparación requiere enteros pero se encontraron ")
                               .append(tipo).append(" y ").append(derecho.tipo).append(".\n");
                        return new ResultadoExpr(null, null, false);
                    }
                    return new ResultadoExpr("boolean", null, false);
                }
            }
            return new ResultadoExpr(tipo, null, false);
        }

        // Número
        if (primero.getCodigo() == 8) { // NUM
            return new ResultadoExpr("int", primero.getToken(), true);
        }

        // Booleanos
        if (primero.getCodigo() == 6 || primero.getCodigo() == 7) { // true=6, false=7
            return new ResultadoExpr("boolean", primero.getToken(), true);
        }

        return new ResultadoExpr(null, null, false);
    }

    public String getErrores() {
        return errores.toString();
    }

    public tablaSimbolos getTablaSimbolos() {
        return tablaSimbolos;
    }
}