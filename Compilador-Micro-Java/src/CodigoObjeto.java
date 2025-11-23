import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.HashMap;

public class CodigoObjeto {

    /**
     * Primera pasada: recibe todo el código intermedio en un único String,
     * lo separa en líneas y devuelve el ArrayList<TokenIntermedio>.
     * Si tienes una tabla de símbolos ya procesada, pásala en 'ts' para que
     * cada token pueda resolver tamaños de memoria.
     *
     * Nota: ignora todo hasta que se encuentre la directiva ".CODE"
     * (case-insensitive). Sólo se procesan líneas dentro del segmento .CODE.
     * Además, se ignoran todas las instrucciones/directivas que empiezan por '.',
     * excepto la directiva .CODE que se usa solo para detectar el inicio del segmento.
     */
    public ArrayList<TokenIntermedio> primeraPasadaTokens(String codigo, tablaSimbolos ts) {
        ArrayList<TokenIntermedio> tokens = new ArrayList<>();
        if (codigo == null) return tokens;

        List<String> lines = Arrays.asList(codigo.split("\\r?\\n"));

        Pattern labelOnly = Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)\\s*:\\s*(.*)$");
        Pattern varDecl = Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)\\s+(DB|DW)\\b\\s*(.*)$", Pattern.CASE_INSENSITIVE);
        Pattern procWithLabel = Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)\\s+PROC\\b(.*)$", Pattern.CASE_INSENSITIVE);

        boolean inCode = false; // <-- ignorar todo hasta encontrar .CODE

        for (String raw : lines) {
            if (raw == null) continue;
            String line = raw.trim();
            if (line.isEmpty()) continue;

            // eliminar comentarios (; o //)
            int idxSemi = line.indexOf(';');
            int idxSlash = line.indexOf("//");
            int commentIdx = Integer.MAX_VALUE;
            if (idxSemi >= 0) commentIdx = Math.min(commentIdx, idxSemi);
            if (idxSlash >= 0) commentIdx = Math.min(commentIdx, idxSlash);
            if (commentIdx != Integer.MAX_VALUE) {
                line = line.substring(0, commentIdx).trim();
                if (line.isEmpty()) continue;
            }

            // Si aún no estamos dentro de .CODE, buscar la directiva y saltar todo lo demás
            if (!inCode) {
                String up = line.toUpperCase();
                if (up.startsWith(".CODE")) {
                    inCode = true;
                    // no crear token para .CODE — solo marcar inicio del segmento
                }
                // ignorar cualquier cosa antes de .CODE (incluye .DATA, declaraciones, etc.)
                continue;
            }

            // A partir de aquí, solo procesar líneas dentro de .CODE

            // Ignorar cualquier directiva/instrucción que empiece por '.'
            if (line.startsWith(".")) {
                continue;
            }

            // Detectar label con ':' al inicio de la línea (o en medio)
            Matcher mLabel = labelOnly.matcher(line);
            if (mLabel.matches()) {
                String label = mLabel.group(1);
                String rest  = mLabel.group(2).trim();
                TokenIntermedio tLabel = new TokenIntermedio("LABEL", label);
                if (ts != null) tLabel.setTablaSimbolos(ts);
                tokens.add(tLabel);
                if (rest.isEmpty()) continue;
                line = rest;
            }

            // Detectar declaración de variable (DB/DW) — dentro de .CODE normalmente no interesa,
            // pero mantener la detección por si aparece y el usuario quiere verla como token.
            Matcher mVar = varDecl.matcher(line);
            if (mVar.matches()) {
                String nombre = mVar.group(1);
                String tipo   = mVar.group(2).toUpperCase();
                String resto  = mVar.group(3).trim();
                TokenIntermedio tv = new TokenIntermedio(tipo, nombre, resto.isEmpty() ? "?" : resto);
                if (ts != null) tv.setTablaSimbolos(ts);
                tokens.add(tv);
                continue;
            }

            // Detectar "IDENT PROC ..." (ej: MAIN PROC FAR) -> crear label + PROC token
            Matcher mProcLabel = procWithLabel.matcher(line);
            if (mProcLabel.matches()) {
                String lbl = mProcLabel.group(1);
                String resto = mProcLabel.group(2).trim();
                TokenIntermedio tLabel = new TokenIntermedio("LABEL", lbl);
                if (ts != null) tLabel.setTablaSimbolos(ts);
                tokens.add(tLabel);
                TokenIntermedio tProc = resto.isEmpty() ? new TokenIntermedio("PROC") : new TokenIntermedio("PROC", resto);
                if (ts != null) tProc.setTablaSimbolos(ts);
                tokens.add(tProc);
                continue;
            }

            // Instrucción / línea normal: separar instrucción del resto
            String[] parts = line.split("\\s+", 2);
            String instr = parts[0].toUpperCase();
            String operands = (parts.length > 1) ? parts[1].trim() : null;

            if (operands == null || operands.isEmpty()) {
                TokenIntermedio t = new TokenIntermedio(instr);
                if (ts != null) t.setTablaSimbolos(ts);
                tokens.add(t);
            } else {
                String[] ops = operands.split(",");
                if (ops.length == 1) {
                    String op = ops[0].trim();
                    TokenIntermedio t = new TokenIntermedio(instr, op);
                    if (ts != null) t.setTablaSimbolos(ts);
                    tokens.add(t);
                } else {
                    String dest = ops[0].trim();
                    String src  = ops[1].trim();
                    TokenIntermedio t = new TokenIntermedio(instr, dest, src);
                    if (ts != null) t.setTablaSimbolos(ts);
                    tokens.add(t);
                }
            }
        }

        return tokens;
    }

    /** Sobrecarga sin tabla de símbolos */
    public ArrayList<TokenIntermedio> primeraPasadaTokens(String codigo) {
        return primeraPasadaTokens(codigo, null);
    }
    public ArrayList<TokenTraducido> traducirAObjeto(ArrayList<TokenIntermedio> tokensIntermedios, tablaSimbolos ts) {
        return null;
    }
}
