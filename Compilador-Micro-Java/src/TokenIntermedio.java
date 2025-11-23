public class TokenIntermedio {
    private String intruccion;
    private String destino;
    private String fuente;
    private String tipoDir; // reg-mem mem-imm reg-reg reg-imm label reg mem
    private Integer tamanioDireccionamiento; // 8 o 16 (null si no determinado)

    private String [] registros16Bits = {"AX","BX","CX","DX","SI","DI","SP","BP"};
    private String [] registros8Bits  = {"AL","BL","CL","DL","AH","BH","CH","DH"};

    // Referencia a la tabla de símbolos que ya tienes (contiene variables en memoria con tipo "boolean" o "int")
    private tablaSimbolos tablaSim = null;

    public TokenIntermedio(String intruccion, String destino, String fuente) {
        this.intruccion = intruccion;
        this.destino = destino;
        this.fuente = fuente;
        setTipoDir();
    }
    public TokenIntermedio(String intruccion, String destino) {
        this.intruccion = intruccion;
        this.destino = destino;
        this.fuente = null;
        setTipoDir();
    }
    public TokenIntermedio(String intruccion) {
        this.intruccion = intruccion;
        this.destino = null;
        this.fuente = null;
        setTipoDir();
    }

    // Inyectar la tabla de símbolos procesada desde fuera
    public void setTablaSimbolos(tablaSimbolos ts) {
        this.tablaSim = ts;
        setTipoDir(); // recalcular tamaño/tipo si ya hay operandos
    }

    public String getDestino() {
        return destino;
    }
    public String getFuente() {
        return fuente;
    }
    public String getIntruccion() {
        return intruccion;
    }
    public String getTipoDir() {
        return tipoDir;
    }
    public Integer getTamanioDireccionamiento() {
        return tamanioDireccionamiento;
    }

    private void setTipoDir() {
        // reset
        tipoDir = null;
        tamanioDireccionamiento = null;

        // Si no hay destino ni fuente, dejar tipoDir nulo
        if (destino == null && fuente == null) {
            return;
        }

        // Caso solo destino: label, reg, mem
        if (fuente == null) {
            if (destino != null && isRegister(destino)) {
                tipoDir = "reg";
                tamanioDireccionamiento = getRegisterSize(destino);
            } else if (destino != null && isLabel(destino)) {
                // Puede ser etiqueta de código o variable en memoria; priorizar memoria si existe en tabla
                Integer szMem = getMemVarSize(destino);
                if (szMem != null) {
                    tipoDir = "mem";
                    tamanioDireccionamiento = szMem;
                } else {
                    tipoDir = "label";
                    tamanioDireccionamiento = null;
                }
            } else {
                // Operando de memoria directo (por ejemplo [var] o var)
                tipoDir = "mem";
                Integer sz = getMemVarSizeFromOperand(destino);
                tamanioDireccionamiento = (sz != null) ? sz : 16; // por defecto 16 si no se conoce
            }
            return;
        }

        // Ambos presentes: reg-reg, reg-mem, reg-imm, mem-reg, mem-imm
        boolean destinoEsReg = destino != null && isRegister(destino);
        boolean fuenteEsReg  = fuente != null && isRegister(fuente);
        boolean fuenteEsImm  = fuente != null && isImmediate(fuente);
        boolean destinoEsImm = destino != null && isImmediate(destino);

        if (destinoEsReg && fuenteEsReg) {
            tipoDir = "reg-reg";
            tamanioDireccionamiento = Math.max(getRegisterSize(destino), getRegisterSize(fuente));
        } else if (destinoEsReg && fuenteEsImm) {
            tipoDir = "reg-imm";
            tamanioDireccionamiento = getRegisterSize(destino);
        } else if (destinoEsReg && !fuenteEsReg && !fuenteEsImm) {
            // destino reg, fuente memoria
            tipoDir = "reg-mem";
            Integer szFuente = getMemVarSizeFromOperand(fuente);
            tamanioDireccionamiento = Math.max(getRegisterSize(destino), (szFuente != null) ? szFuente : 16);
        } else if (!destinoEsReg && fuenteEsReg) {
            // destino memoria, fuente reg
            tipoDir = "mem-reg";
            Integer szDestino = getMemVarSizeFromOperand(destino);
            tamanioDireccionamiento = Math.max((szDestino != null) ? szDestino : 16, getRegisterSize(fuente));
        } else if (!destinoEsReg && fuenteEsImm) {
            tipoDir = "mem-imm";
            Integer szDestino = getMemVarSizeFromOperand(destino);
            tamanioDireccionamiento = (szDestino != null) ? szDestino : 16;
        } else if (destinoEsImm && fuenteEsReg) {
            // raro, pero tratar como reg-imm usando el registro como destino lógico
            tipoDir = "reg-imm";
            tamanioDireccionamiento = getRegisterSize(fuente);
        } else {
            // fallback: ambos memoria o no reconocidos
            if (fuenteEsImm) {
                tipoDir = "mem-imm";
                tamanioDireccionamiento = getMemVarSizeFromOperand(destino);
                if (tamanioDireccionamiento == null) tamanioDireccionamiento = 16;
            } else {
                tipoDir = "mem-reg";
                Integer s1 = getMemVarSizeFromOperand(destino);
                Integer s2 = getMemVarSizeFromOperand(fuente);
                int a = (s1 != null) ? s1 : 16;
                int b = (s2 != null) ? s2 : 16;
                tamanioDireccionamiento = Math.max(a, b);
            }
        }
    }

    private boolean isRegister(String s) {
        if (s == null) return false;
        String val = s.trim().toUpperCase();
        for (String reg : registros16Bits) if (reg.equals(val)) return true;
        for (String reg : registros8Bits)  if (reg.equals(val)) return true;
        return false;
    }

    private int getRegisterSize(String s) {
        if (s == null) return 0;
        String val = s.trim().toUpperCase();
        for (String reg : registros16Bits) if (reg.equals(val)) return 16;
        for (String reg : registros8Bits)  if (reg.equals(val)) return 8;
        return 0;
    }

    private boolean isImmediate(String s) {
        if (s == null) return false;
        String val = s.trim();
        return val.matches("-?\\d+");
    }

    private boolean isLabel(String s) {
        if (s == null) return false;
        String val = s.trim();
        return val.matches("[A-Za-z_][A-Za-z0-9_]*");
    }

    // Busca en la tabla de símbolos el tamaño de la variable: boolean -> 8, int -> 16
    private Integer getMemVarSize(String nombre) {
        if (nombre == null || tablaSim == null) return null;
        String n = nombre.trim();
        if (!tablaSim.existe(n)) return null;
        Simbolo sim = tablaSim.getSimbolo(n);
        if (sim == null) return null;
        // Se asume que Simbolo tiene un método getTipo() que devuelve "boolean" o "int"
        try {
            String tipo = sim.getTipo(); // ajustar si el método se llama distinto
            if (tipo == null) return null;
            tipo = tipo.trim().toLowerCase();
            if ("boolean".equals(tipo)) return 8;
            if ("int".equals(tipo)) return 16;
        } catch (Exception e) {
            // Si Simbolo no tiene getTipo(), intentar inspección por toString() (fallback mínimo)
            String s = sim.toString().toLowerCase();
            if (s.contains("boolean")) return 8;
            if (s.contains("int")) return 16;
        }
        return null;
    }

    // Extrae posible nombre de variable desde un operando de memoria y devuelve su tamaño (o null si no se encuentra)
    private Integer getMemVarSizeFromOperand(String operand) {
        if (operand == null) return null;
        String o = operand.trim();
        // Quitar corchetes si existen: [var], [var+4], var
        if (o.startsWith("[") && o.endsWith("]")) {
            o = o.substring(1, o.length() - 1).trim();
        }
        // Tomar el primer token que parezca un identificador (antes de +, - o espacios)
        String[] parts = o.split("[+\\-*/\\s]");
        for (String p : parts) {
            p = p.trim();
            if (p.length() == 0) continue;
            if (isLabel(p)) {
                Integer sz = getMemVarSize(p);
                if (sz != null) return sz;
            }
        }
        // Intentar coincidencia completa
        Integer direct = getMemVarSize(o);
        if (direct != null) return direct;
        return null;
    }
}
