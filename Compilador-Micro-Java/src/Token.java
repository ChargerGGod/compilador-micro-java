public class Token {
    private String lexema;
    private String token;
    private int codigo;
    public Token(String lexema,String token,int codigo){
        this.lexema = lexema;
        this.token = token;
        this.codigo = codigo;
    }
    public String getLexema() {
        return lexema;
    }
    public String getToken() {
        return token;
    }
    public int getCodigo() {
        return codigo;
    }
    @Override
    public String toString() {
        return token;
    }
}
