class Simbolo {
    private String identificador;
    private String tipo;
    private String valor;
    private int direccion;

    public Simbolo(String identificador, String tipo, String valor, int direccion) {
        this.identificador = identificador;
        this.direccion = direccion;
        this.tipo = tipo;
        this.valor = valor;
    }

    public String getIdentificador() {
        return identificador;
    }

    public String getTipo() {
        return tipo;
    }

    public String getValor() {
        return valor;
    }
    public int getDireccion() {
        return direccion;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    @Override
    public String toString(){
        String valorStr = (valor == null) ? "sin valor" : valor;
        return identificador + ", " + tipo + ", " + valorStr + ", " + direccion;
    }
}