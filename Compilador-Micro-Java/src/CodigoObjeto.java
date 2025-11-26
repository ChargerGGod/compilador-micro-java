/* Alumno: Kevin Ignacio Rojas Duarte
 * NC: 22170801
 * Materia: Lenguajes y automatas 2
 * Profesor: Rosalio Zatarain Cabada
 * Hora: 13:00pm - 14:00pm
 */
import java.util.*;
import java.util.regex.*;

public class CodigoObjeto {

    // Patrones Regex pre-compilados
    private static final Pattern P_LABEL = Pattern.compile("^([A-Za-z_]\\w*):\\s*(.*)$");
    private static final Pattern P_VAR   = Pattern.compile("^([A-Za-z_]\\w*)\\s+(DB|DW)\\b\\s*(.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern P_PROC  = Pattern.compile("^([A-Za-z_]\\w*)\\s+PROC\\b(.*)$", Pattern.CASE_INSENSITIVE);

    //FASE 1: PARSER (Convierte texto a Tokens)
    public ArrayList<TokenIntermedio> primeraPasadaTokens(String codigo, tablaSimbolos ts) {
        ArrayList<TokenIntermedio> tokens = new ArrayList<>();
        if (codigo == null) return tokens;

        String[] lines = codigo.split("\\r?\\n");
        boolean inCode = false;

        for (String raw : lines) {
            String line = raw.split(";")[0].split("//")[0].trim(); // Quitar comentarios
            if (line.isEmpty()) continue;

            if (!inCode) {
                if (line.toUpperCase().startsWith(".CODE")) inCode = true;
                continue;
            }
            if (line.startsWith(".")) continue;

            // Detectar Labels
            Matcher mLabel = P_LABEL.matcher(line);
            if (mLabel.matches()) {
                TokenIntermedio t = new TokenIntermedio("LABEL", mLabel.group(1));
                if (ts != null) t.setTablaSimbolos(ts);
                tokens.add(t);
                line = mLabel.group(2).trim();
                if (line.isEmpty()) continue;
            }

            // Detectar PROC
            Matcher mProc = P_PROC.matcher(line);
            if (mProc.matches()) {
                TokenIntermedio tLabel = new TokenIntermedio("LABEL", mProc.group(1));
                if (ts != null) tLabel.setTablaSimbolos(ts);
                tokens.add(tLabel);
                tokens.add(new TokenIntermedio("PROC"));
                continue;
            }

            // Detectar Variables en Code (poco común pero soportado)
            Matcher mVar = P_VAR.matcher(line);
            if (mVar.matches()) {
                String val = mVar.group(3).isEmpty() ? "?" : mVar.group(3);
                TokenIntermedio t = new TokenIntermedio(mVar.group(2).toUpperCase(), mVar.group(1), val);
                if (ts != null) t.setTablaSimbolos(ts);
                tokens.add(t);
                continue;
            }

            // Detectar Instrucciones
            String[] parts = line.split("\\s+", 2);
            String instr = parts[0].toUpperCase();
            String args = (parts.length > 1) ? parts[1].trim() : "";
            TokenIntermedio t;
            if (args.isEmpty()) {
                t = new TokenIntermedio(instr);
            } else {
                String[] ops = args.split(",");
                if (ops.length == 1) t = new TokenIntermedio(instr, ops[0].trim());
                else t = new TokenIntermedio(instr, ops[0].trim(), ops[1].trim());
            }
            if (ts != null) t.setTablaSimbolos(ts);
            tokens.add(t);
        }
        return tokens;
    }

    public ArrayList<TokenIntermedio> primeraPasadaTokens(String codigo) {
        return primeraPasadaTokens(codigo, null);
    }

    /**
     * FASE 2: TRADUCTOR (Calcula Offsets y Genera Binario)
     */
    public ArrayList<TokenTraducido> traducirAObjeto(ArrayList<TokenIntermedio> tokensIntermedios, tablaSimbolos ts) {
        ArrayList<TokenTraducido> salida = new ArrayList<>();
        if (tokensIntermedios == null) return salida;

        // 1. Mapa de Datos (Offsets de variables)
        Map<String, Integer> dataOffsets = new HashMap<>();
        Map<String, Simbolo> tabla = (ts != null) ? ts.getTabla() : new HashMap<>();
        int dataOffset = 0;
        
        for (Simbolo s : tabla.values()) {
            boolean isByte = s.getTipo() != null && s.getTipo().equalsIgnoreCase("boolean");
            int size = isByte ? 1 : 2;
            dataOffsets.put(s.getIdentificador(), dataOffset);
            
            int val = parseImmediateValue(s.getValor());
            String bin = byteToBin(val & 0xFF);
            if (size == 2) bin += " " + byteToBin((val >> 8) & 0xFF);
            
            salida.add(new TokenTraducido(
                String.format("DATA+%04Xh", dataOffset), 
                bin, 
                (isByte ? "DB " : "DW ") + s.getIdentificador()
            ));
            dataOffset += size;
        }

        // 2. Calcular Offsets de Etiquetas (Labels)
        Map<String, Integer> labelOffsets = new HashMap<>();
        int ip = 0;
        for (TokenIntermedio t : tokensIntermedios) {
            if ("LABEL".equalsIgnoreCase(t.getIntruccion())) {
                labelOffsets.put(t.getDestino(), ip);
            } else {
                ip += calcularTamanoInstruccion(t);
            }
        }

        // 3. Generación de Código
        ip = 0;
        for (TokenIntermedio t : tokensIntermedios) {
            String instr = t.getIntruccion().toUpperCase();
            
            // Labels solo se muestran, no generan bytes
            if (instr.equals("LABEL")) {
                salida.add(new TokenTraducido(String.format("CODE+%04Xh", ip), "", t.getDestino() + ":"));
                continue;
            }
            // Directivas que no generan código
            if (instr.equals("PROC") || instr.endsWith("ENDP")) {
                continue;
            }

            // Lista de Strings para permitir formato [xx xx]
            List<String> bytes = new ArrayList<>();
            String human = instr + " " + (t.getDestino() != null ? t.getDestino() : "") + (t.getFuente() != null ? ", " + t.getFuente() : "");

            switch (instr) {
                case "MOV": emitirALU(t, bytes, 0x88, 0xB0, 0xC6, dataOffsets); break;
                case "ADD": emitirALU(t, bytes, 0x00, 0x80, 0x80, dataOffsets); break;
                case "SUB": emitirALU(t, bytes, 0x28, 0x80, 0x80, dataOffsets); break;
                case "XOR": emitirALU(t, bytes, 0x30, 0x80, 0x80, dataOffsets); break;
                case "CMP": emitirALU(t, bytes, 0x38, 0x80, 0x80, dataOffsets); break;
                
                case "JMP":  emitirSalto(t, bytes, 0xE9, labelOffsets, ip); break;
                case "CALL": emitirSalto(t, bytes, 0xE8, labelOffsets, ip); break;
                case "LOOP": emitirLoop(t, bytes, labelOffsets, ip); break;
                
                case "JNE": emitirJcc(t, bytes, 0x85, labelOffsets, ip); break;
                case "JE":  emitirJcc(t, bytes, 0x84, labelOffsets, ip); break;
                case "JG":  emitirJcc(t, bytes, 0x8F, labelOffsets, ip); break;
                case "JGE": emitirJcc(t, bytes, 0x8D, labelOffsets, ip); break;
                case "JL":  emitirJcc(t, bytes, 0x8C, labelOffsets, ip); break;
                case "JLE": emitirJcc(t, bytes, 0x8E, labelOffsets, ip); break;

                case "PUSH": emitirPushPop(t, bytes, 0x50); break;
                case "POP":  emitirPushPop(t, bytes, 0x58); break;
                case "INC":  emitirIncDec(t, bytes, 0x40, dataOffsets); break;
                case "DEC":  emitirIncDec(t, bytes, 0x48, dataOffsets); break;
                
                // MUL y DIV (Corregidos para usar Destino como operando único)
                case "MUL":  emitirMulDiv(t, bytes, 4, dataOffsets); break; 
                case "DIV":  emitirMulDiv(t, bytes, 6, dataOffsets); break; 

                case "RET": addByte(bytes, 0xC3); break;
                case "INT": 
                    addByte(bytes, 0xCD); 
                    addByte(bytes, parseImmediateValue(t.getDestino())); 
                    break;

                default: addByte(bytes, 0x90); human += " (NOP)";
            }

            StringBuilder binStr = new StringBuilder();
            for (String b : bytes) binStr.append(b).append(" ");
            
            if (esSoloNops(bytes)) {
                // actualizar IP igual que siempre (la instrucción sigue existiendo en el tamaño)
                ip += calcularTamanoInstruccion(t);
                continue;
            }
            
            salida.add(new TokenTraducido(String.format("CODE+%04Xh", ip), binStr.toString().trim(), human));
            
            // Actualizar IP con el tamaño real calculado
            ip += calcularTamanoInstruccion(t);
        }
        return salida;
    }
    
    // Comprueba si la lista de bytes representa únicamente NOPs (0x90).
    private boolean esSoloNops(List<String> bytes) {
        if (bytes == null || bytes.isEmpty()) return false;
        String nop = byteToBin(0x90);
        for (String b : bytes) {
            if (b == null) return false;
            String t = b.trim();
            if (t.startsWith("[")) return false;
            if (!t.equals(nop)) return false;
        }
        return true;
    }
    
    // MÉTODOS HELPERS DE EMISIÓN
    private void emitirALU(TokenIntermedio t, List<String> out, int baseRegReg, int baseImmReg, int baseImmMem, Map<String, Integer> dataOffsets) {
        String dst = t.getDestino();
        String src = t.getFuente();
        boolean dstReg = isRegister(dst);
        boolean srcReg = isRegister(src);
        boolean srcImm = isImmediate(src);

        if (dstReg && srcReg) { // Reg, Reg
            addByte(out, baseRegReg | 1);
            addByte(out, (0b11 << 6) | ((regCode(src) & 7) << 3) | (regCode(dst) & 7));
        } 
        else if (dstReg && !srcReg && !srcImm) { // Reg, Mem
            addByte(out, baseRegReg | 2 | 1);
            addByte(out, (0b00 << 6) | ((regCode(dst) & 7) << 3) | 0b110);
            addDataOffset(out, dataOffsets.getOrDefault(strip(src), 0));
        }
        else if (!dstReg && srcReg) { // Mem, Reg
            addByte(out, baseRegReg | 1);
            addByte(out, (0b00 << 6) | ((regCode(src) & 7) << 3) | 0b110);
            addDataOffset(out, dataOffsets.getOrDefault(strip(dst), 0));
        }
        else if (dstReg && srcImm) { // Reg, Imm
            if (t.getIntruccion().equalsIgnoreCase("MOV")) {
                addByte(out, baseImmReg | 8 | (regCode(dst) & 7));
                addWord(out, parseImmediateValue(src));
            } else {
                addByte(out, 0x81);
                int ext = getAluExtension(t.getIntruccion());
                addByte(out, (0b11 << 6) | (ext << 3) | (regCode(dst) & 7));
                addWord(out, parseImmediateValue(src));
            }
        }
        else if (!dstReg && srcImm) { // Mem, Imm
            int opcode = t.getIntruccion().equalsIgnoreCase("MOV") ? 0xC7 : 0x81;
            addByte(out, opcode);
            int ext = t.getIntruccion().equalsIgnoreCase("MOV") ? 0 : getAluExtension(t.getIntruccion());
            addByte(out, (0b00 << 6) | ((ext & 7) << 3) | 0b110);
            addDataOffset(out, dataOffsets.getOrDefault(strip(dst), 0));
            addWord(out, parseImmediateValue(src));
        }
        else {
            addByte(out, 0x90); // Fallback NOP
        }
    }

    private void emitirSalto(TokenIntermedio t, List<String> out, int opcode, Map<String, Integer> labels, int currentIp) {
        addByte(out, opcode);
        int target = labels.getOrDefault(t.getDestino(), 0);
        int disp = target - (currentIp + 3);
        addWord(out, disp);
    }

    private void emitirJcc(TokenIntermedio t, List<String> out, int opcode, Map<String, Integer> labels, int currentIp) {
        addByte(out, 0x0F);
        addByte(out, opcode);
        int target = labels.getOrDefault(t.getDestino(), 0);
        int disp = target - (currentIp + 4);
        addWord(out, disp);
    }

    private void emitirLoop(TokenIntermedio t, List<String> out, Map<String, Integer> labels, int currentIp) {
        addByte(out, 0xE2);
        int target = labels.getOrDefault(t.getDestino(), 0);
        int disp = target - (currentIp + 2);
        addByte(out, disp & 0xFF);
    }

    private void emitirPushPop(TokenIntermedio t, List<String> out, int base) {
        if (isRegister(t.getDestino())) {
            addByte(out, base | (regCode(t.getDestino()) & 7));
        } else {
            addByte(out, 0x90);
        }
    }

    private void emitirIncDec(TokenIntermedio t, List<String> out, int baseReg, Map<String, Integer> dataOffsets) {
        if (isRegister(t.getDestino())) {
            addByte(out, baseReg | (regCode(t.getDestino()) & 7));
        } else {
            addByte(out, 0xFF);
            int ext = (t.getIntruccion().equalsIgnoreCase("INC")) ? 0 : 1;
            addByte(out, (0b00 << 6) | (ext << 3) | 0b110);
            addDataOffset(out, dataOffsets.getOrDefault(strip(t.getDestino()), 0));
        }
    }

    private void emitirMulDiv(TokenIntermedio t, List<String> out, int ext, Map<String, Integer> dataOffsets) {
        addByte(out, 0xF7);
        // Verificar DESTINO porque en MUL/DIV de un operando, el parser lo pone en destino
        if (isRegister(t.getDestino())) {
            addByte(out, (0b11 << 6) | (ext << 3) | (regCode(t.getDestino()) & 7));
        } else {
            addByte(out, (0b00 << 6) | (ext << 3) | 0b110);
            addDataOffset(out, dataOffsets.getOrDefault(strip(t.getDestino()), 0));
        }
    }

    // CÁLCULO DE TAMAÑO
    private int calcularTamanoInstruccion(TokenIntermedio t) {
        String instr = t.getIntruccion().toUpperCase();
        if (instr.equals("LABEL") || instr.equals("PROC") || instr.endsWith("ENDP")) return 0;

        boolean dstReg = isRegister(t.getDestino());
        boolean srcReg = isRegister(t.getFuente());
        boolean srcImm = isImmediate(t.getFuente());
        boolean dstMem = !dstReg && t.getDestino() != null; 

        switch (instr) {
            case "RET": case "NOP": return 1;
            case "INT": return 2;
            case "PUSH": case "POP": return 1;
            case "INC": case "DEC": return dstReg ? 1 : 4; 
            case "JMP": case "CALL": return 3;
            case "LOOP": return 2;
            case "JNE": case "JE": case "JG": case "JGE": case "JL": case "JLE": return 4;

            case "MOV": case "ADD": case "SUB": case "CMP": case "XOR":
                if (dstReg && srcReg) return 2;
                if ((dstMem && srcReg) || (dstReg && !srcReg && !srcImm)) return 4;
                if (dstReg && srcImm) return instr.equals("MOV") ? 3 : 4;
                if (dstMem && srcImm) return 6;
                return 1;

            case "MUL": case "DIV": return dstReg ? 2 : 4; // Corregido: verifica destino
            default: return 1;
        }
    }
    
    // UTILS
    private void addByte(List<String> out, int val) {
        out.add(byteToBin(val));
    }

    private void addWord(List<String> out, int val) {
        addByte(out, val & 0xFF);
        addByte(out, (val >> 8) & 0xFF);
    }

    private void addDataOffset(List<String> out, int val) {
        String low = byteToBin(val & 0xFF);
        String high = byteToBin((val >> 8) & 0xFF);
        out.add("[" + low + " " + high + "]");
    }

    private String byteToBin(int val) {
        String s = Integer.toBinaryString(val & 0xFF);
        while (s.length() < 8) s = "0" + s;
        return s;
    }

    private int parseImmediateValue(String s) {
        if (s == null) return 0;
        s = s.trim().toUpperCase();
        try {
            if (s.startsWith("'") && s.length() >= 3) return (int) s.charAt(1);
            if (s.endsWith("H")) return Integer.parseInt(s.replace("H", ""), 16);
            return Integer.parseInt(s);
        } catch (Exception e) { return 0; }
    }

    private boolean isRegister(String s) {
        if (s == null) return false;
        return s.matches("(?i)^(AX|BX|CX|DX|SI|DI|BP|SP|AL|AH|BL|BH|CL|CH|DL|DH)$");
    }

    private boolean isImmediate(String s) {
        return s != null && (Character.isDigit(s.charAt(0)) || s.startsWith("'") || s.startsWith("-"));
    }

    private String strip(String s) {
        if (s == null) return "";
        return s.replaceAll("[\\[\\]]", "").trim();
    }

    private int regCode(String s) {
        if (s == null) return 0;
        s = s.toUpperCase();
        if (s.contains("A")) return 0; if (s.contains("C")) return 1;
        if (s.contains("D")) return 2; if (s.contains("B")) return 3;
        if (s.contains("SP")) return 4; if (s.contains("BP")) return 5;
        if (s.contains("SI")) return 6; if (s.contains("DI")) return 7;
        return 0;
    }

    private int getAluExtension(String instr) {
        switch(instr) { case "ADD": return 0; case "OR": return 1; case "SUB": return 5; case "CMP": return 7; case "XOR": return 6; default: return 0; }
    }
}