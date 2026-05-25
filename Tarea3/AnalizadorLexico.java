public class AnalizadorLexico {
    private final String fuente;
    private int pos;
    private int linea;

    public AnalizadorLexico(String fuente) {
        this.fuente = fuente;
        this.pos    = 0;
        this.linea  = 1;
    }

    public Token siguienteToken() {
        saltarEspaciosYComentarios();

        if (pos >= fuente.length()) {
            return new Token(TipoToken.EOF, "EOF", linea);
        }

        char c = actual();

        switch (c) {
            case '[': avanzar(); return new Token(TipoToken.L_CORCHETE,  "[", linea);
            case ']': avanzar(); return new Token(TipoToken.R_CORCHETE,  "]", linea);
            case '{': avanzar(); return new Token(TipoToken.L_LLAVE,     "{", linea);
            case '}': avanzar(); return new Token(TipoToken.R_LLAVE,     "}", linea);
            case ',': avanzar(); return new Token(TipoToken.COMA,        ",", linea);
            case ':': avanzar(); return new Token(TipoToken.DOS_PUNTOS,  ":", linea);
            case '"': return leerCadena();
        }

        if (Character.isDigit(c)) return leerNumero();
        if (Character.isLetter(c)) return leerPalabraReservada();

        char invalido = avanzar();
        return new Token(TipoToken.ERROR, String.valueOf(invalido), linea);
    }

    private char actual() {
        return (pos < fuente.length()) ? fuente.charAt(pos) : '\0';
    }

    private char avanzar() {
        char c = fuente.charAt(pos++);
        if (c == '\n') linea++;
        return c;
    }

    private void saltarEspaciosYComentarios() {
        while (pos < fuente.length() && Character.isWhitespace(actual())) {
            avanzar();
        }
    }

    private Token leerCadena() {
        int lineaInicio = linea;
        StringBuilder sb = new StringBuilder();
        sb.append(avanzar());

        while (pos < fuente.length() && actual() != '"') {
            if (actual() == '\\') {
                sb.append(avanzar());
                if (pos < fuente.length()) {
                    sb.append(avanzar());
                }
            } else {
                sb.append(avanzar());
            }
        }

        if (pos < fuente.length()) {
            sb.append(avanzar());
        } else {
            return new Token(TipoToken.ERROR, sb.toString(), lineaInicio);
        }

        return new Token(TipoToken.LITERAL_CADENA, sb.toString(), lineaInicio);
    }

    private Token leerNumero() {
        int lineaInicio = linea;
        StringBuilder sb = new StringBuilder();

        while (pos < fuente.length() && Character.isDigit(actual())) {
            sb.append(avanzar());
        }

        if (pos < fuente.length() && actual() == '.') {
            sb.append(avanzar());
            while (pos < fuente.length() && Character.isDigit(actual())) {
                sb.append(avanzar());
            }
        }

        if (pos < fuente.length() && (actual() == 'e' || actual() == 'E')) {
            sb.append(avanzar());
            if (pos < fuente.length() && (actual() == '+' || actual() == '-')) {
                sb.append(avanzar());
            }
            while (pos < fuente.length() && Character.isDigit(actual())) {
                sb.append(avanzar());
            }
        }

        return new Token(TipoToken.LITERAL_NUM, sb.toString(), lineaInicio);
    }

    private Token leerPalabraReservada() {
        int lineaInicio = linea;
        StringBuilder sb = new StringBuilder();

        while (pos < fuente.length() && (Character.isLetterOrDigit(actual()) || actual() == '_')) {
            sb.append(avanzar());
        }

        String palabra = sb.toString();

        switch (palabra.toLowerCase()) {
            case "true":  return new Token(TipoToken.PR_TRUE,  palabra, lineaInicio);
            case "false": return new Token(TipoToken.PR_FALSE, palabra, lineaInicio);
            case "null":  return new Token(TipoToken.PR_NULL,  palabra, lineaInicio);
            default:
                return new Token(TipoToken.ERROR, palabra, lineaInicio);
        }
    }
}
