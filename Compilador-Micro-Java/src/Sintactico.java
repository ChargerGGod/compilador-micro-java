import java.util.ArrayList;

public class Sintactico {
    private ArrayList<Token> tokens;
    public Sintactico(ArrayList<Token> tokens){
        this.tokens = tokens;
    }
    public boolean sintaxisCorrecta() {
        if (tokens.isEmpty() || tokens.get(0).getCodigo() != 1) // class
            return false;
        tokens.remove(0);

        if (!match(9)) // Identifier
            return false;

        if (!match(10)) // {
            return false;

        while (isVarDeclaration()) {
            if (!varDeclaration())
                return false;
        }

        if (!statement())
            return false;

        if (!match(11)) // }
            return false;

        return tokens.isEmpty();
    }

    private boolean isVarDeclaration() {
        if (tokens.isEmpty()) return false;
        int code = tokens.get(0).getCodigo();
        return code == 2 || code == 3; // int or boolean
    }

    private boolean varDeclaration() {
        if (!type()) return false;
        if (!match(9)) return false; // Identifier
        if (!match(14)) return false; // ;
        return true;
    }

    private boolean type() {
        if (tokens.isEmpty()) return false;
        int code = tokens.get(0).getCodigo();
        if (code == 2 || code == 3) { // int or boolean
            tokens.remove(0);
            return true;
        }
        return false;
    }

    private boolean statement() {
        while (!tokens.isEmpty() && !peek(11)) { // while not }
            if (peek(4)) { // while
                tokens.remove(0);
                if (!match(12)) return false; // (
                if (!expression()) return false;
                if (!match(13)) return false; // )
                if (!match(10)) return false; // {
                if (!statement()) return false;
                if (!match(11)) return false; // }
            } else if (peek(5)) { // print
                tokens.remove(0);
                if (!match(12)) return false; // (
                if (!expression()) return false;
                if (!match(13)) return false; // )
                if (!match(14)) return false; // ;
            } else if (peek(9)) { // Identifier
                tokens.remove(0);
                if (!match(15)) return false; // =
                if (!expression()) return false;
                if (!match(14)) return false; // ;
            } else {
                return false;
            }
        }
        return true;
    }

    private boolean expression() {
        if (tokens.isEmpty()) return false;
        if (peek(9) || peek(8)) { // Identifier or Integer
            tokens.remove(0);
            if (isOperator()) {
                tokens.remove(0);
                if (peek(9) || peek(8)) {
                    tokens.remove(0);
                    return true;
                }
                return false;
            }
            return true;
        } else if (peek(6) || peek(7)) { // true or false
            tokens.remove(0);
            return true;
        }
        return false;
    }

    private boolean isOperator() {
        if (tokens.isEmpty()) return false;
        int code = tokens.get(0).getCodigo();
        return code >= 16 && code <= 20 || code == 18 || code == 19; // + - * < >
    }

    private boolean match(int code) {
        if (!tokens.isEmpty() && tokens.get(0).getCodigo() == code) {
            tokens.remove(0);
            return true;
        }
        return false;
    }

    private boolean peek(int code) {
        return !tokens.isEmpty() && tokens.get(0).getCodigo() == code;
    }

}
