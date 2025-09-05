import java.util.ArrayList;
public class Lexico {
    /*
     * class = 1
     * int = 2
     * boolean = 3
     * while = 4
     * print = 5
     * true = 6
     * false = 7
     * NUM = 8
     * ID = 9
     * { = 10
     * } = 11
     * ( = 12
     * ) = 13
     * ; = 14
     * = = 15
     * + = 16
     * - = 17
     * < = 18
     * > = 19
     * * = 20
     * 
     */
    String[] palabrasResevadas = {"class", "int", "boolean", "while", "print", "true", "false"};
    private StringBuilder errores = new StringBuilder();

    public ArrayList<Token> Tokenizar(String stringTokens) {
        ArrayList<Token> tokens = new ArrayList<>();
        String identificador = "";
        int linea = 1;
        for (int i = 0; i < stringTokens.length(); i++) {
            char caracter = stringTokens.charAt(i);

            if (Character.isLetter(caracter) || (Character.isDigit(caracter) && !identificador.equals(""))) {
                identificador += caracter;
            } else if (Character.isDigit(caracter) && identificador.equals("")) {
                String numero = "";
                numero += caracter;
                i++;
                while (i < stringTokens.length() && Character.isDigit(stringTokens.charAt(i))) {
                    numero += stringTokens.charAt(i);
                    i++;
                }
                i--;
                tokens.add(new Token("NUM", numero,8));
            } else {
                if (!identificador.equals("")) {
                    boolean esPalabraReservada = false;
                    for (int j = 0; j< palabrasResevadas.length;j++) {
                        if (identificador.equals(palabrasResevadas[j])) {
                            tokens.add(new Token("PR", identificador,(j+1)));
                            esPalabraReservada = true;
                            break;
                        }
                    }
                    if (!esPalabraReservada) {
                        tokens.add(new Token("ID", identificador,9));
                    }
                    identificador = "";
                }
                switch (caracter) {
                    case '{':
                        tokens.add(new Token("LLAVEAPER", "{",10));
                        break;
                    case '}':
                        tokens.add(new Token("LLAVECERR", "}",11));
                        break;
                    case '(':
                        tokens.add(new Token("PARENTESISAPER", "(",12));
                        break;
                    case ')':
                        tokens.add(new Token("PARENTESISCER", ")",13));
                        break;
                    case ';':
                        tokens.add(new Token("PUNTOYCOMA", ";",14));
                        break;
                    case '=':
                        tokens.add(new Token("IGUAL", "=",15));
                        break;
                    case '+':
                        tokens.add(new Token("MAS", "+",16));
                        break;
                    case '-':
                        tokens.add(new Token("MENOS", "-",17));
                        break;
                    case '<':
                        tokens.add(new Token("MENORQUE", "<",18));
                        break;
                    case '>':
                        tokens.add(new Token("MAYORQUE", ">",19));
                        break;
                    case '*':
                        tokens.add(new Token("MULT", "*",20));
                        break;
                    case '\n':
                        linea++;
                        break;
                    default:
                        if (!Character.isWhitespace(caracter)) {
                            errores.append("Error: Simbolo no reconocido '")
                                   .append(caracter)
                                   .append("' Linea: ")
                                   .append(linea)
                                   .append("\n");
                        }
                        break;
                }
            }
        }
        return tokens;
    }

    public String getErrores() {
        return errores.toString();
    }
}