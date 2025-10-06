import java.util.HashMap;

class tablaSimbolos {
    private HashMap<String, Simbolo> tabla = new HashMap<>();

    public boolean existe(String id) {
        return tabla.containsKey(id);
    }

    public Simbolo getSimbolo(String id) {
        return tabla.get(id);
    }

    public void agregarSimbolo(Simbolo s) {
        tabla.put(s.getIdentificador(), s);
    }

    public HashMap<String, Simbolo> getTabla() {
        return tabla;
    }

    @Override
    public String toString(){
        String txt = "";
        for (Simbolo simbolo: tabla.values()) {
            txt += simbolo.toString() + "\n";
        }
        return txt;
    }
}