import java.util.ArrayList;

public class App {
    public static void main(String[] args) throws Exception {
        Lexico lexico = new Lexico();
        String codigo = "class SUMA { int x; x1 = 15; x = x * 2; print(x); }";
        ArrayList<Token> tokens = lexico.Tokenizar(codigo);
        for (Token token : tokens) {
            System.out.println(token.getLexema() + " : " + token.getToken());
        }
    }
}
