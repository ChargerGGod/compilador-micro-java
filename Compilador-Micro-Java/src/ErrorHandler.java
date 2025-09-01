import java.util.ArrayList;

public class ErrorHandler {
    private ArrayList<String> errores;
    public String mensajeError;
    public int lineaError;
    public int columnaError;
    public ErrorHandler(){
        this.errores = new ArrayList<>();
    }
    public void setColumnaError(int columnaError) {
        this.columnaError = columnaError;
    }
    public void setErrores(String mensajeError,int lineaError,int columnaError) {
        this.mensajeError = mensajeError;
        this.lineaError = lineaError;
        this.columnaError = columnaError;
        this.errores.add("Error: "+mensajeError+" Linea: "+lineaError+" Columna: "+columnaError);
    }
    public void setLineaError(int lineaError) {
        this.lineaError = lineaError;
    }
    public ArrayList<String> getErrores() {
        return errores;
    }
}
