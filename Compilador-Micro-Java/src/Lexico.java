import java.util.ArrayList;
public class Lexico {
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
                tokens.add(new Token("NUM", numero));
            } else {
                if (!identificador.equals("")) {
                    boolean esPalabraReservada = false;
                    for (String palabraReservada : palabrasResevadas) {
                        if (identificador.equals(palabraReservada)) {
                            tokens.add(new Token("PR", identificador));
                            esPalabraReservada = true;
                            break;
                        }
                    }
                    if (!esPalabraReservada) {
                        tokens.add(new Token("ID", identificador));
                    }
                    identificador = "";
                }
                switch (caracter) {
                    case '{':
                        tokens.add(new Token("LLAVEAPER", "{"));
                        break;
                    case '}':
                        tokens.add(new Token("LLAVECERR", "}"));
                        break;
                    case '(':
                        tokens.add(new Token("PARENTESISAPER", "("));
                        break;
                    case ')':
                        tokens.add(new Token("PARENTESISCER", ")"));
                        break;
                    case ';':
                        tokens.add(new Token("PUNTOYCOMA", ";"));
                        break;
                    case '=':
                        tokens.add(new Token("IGUAL", "="));
                        break;
                    case '+':
                        tokens.add(new Token("MAS", "+"));
                        break;
                    case '-':
                        tokens.add(new Token("MENOS", "-"));
                        break;
                    case '<':
                        tokens.add(new Token("MENORQUE", "<"));
                        break;
                    case '>':
                        tokens.add(new Token("MAYORQUE", ">"));
                        break;
                    case '*':
                        tokens.add(new Token("MULT", "*"));
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