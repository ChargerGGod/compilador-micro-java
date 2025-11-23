public class TokenTraducido {
    private String offset;
    private String binario;
    private String instruccion;
    public TokenTraducido(String offset, String binario, String instruccion) {
        this.offset = offset;
        this.binario = binario;
        this.instruccion = instruccion;
    }
    public String getBinario() {
        return binario;
    }
    public String getInstruccion() {
        return instruccion;
    }
    public String getOffset() {
        return offset;
    }
    @Override
    public String toString() {
        return offset + " " + binario + " " + instruccion;
    }
}
