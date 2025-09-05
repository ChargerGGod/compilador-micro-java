import java.util.ArrayList;

public class Sintactico {
    private ArrayList<Token> tokens;
    public Sintactico(ArrayList<Token> tokens){
        this.tokens = tokens;
    }
    public boolean sintaxisCorrecta(){
        boolean correcto = true;
        if (tokens.get(0).getCodigo()!=1)
            return false;
        tokens.remove(0);

        return correcto;
    }

    

}
