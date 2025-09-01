public class Token {
    private String lexema;
    private String token;
    public Token(String lexema,String token){
        this.lexema = lexema;
        this.token = token;
    }
    public String getLexema() {
        return lexema;
    }
    public String getToken() {
        return token;
    }
}
