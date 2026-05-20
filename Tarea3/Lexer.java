import java.util.ArrayList;
import java.util.List;

public class Lexer {
    private final String source;
    private int pos;
    private int line;
    private int col;
    private final List<String> errors;

    public Lexer(String source) {
        this.source = source;
        this.pos = 0;
        this.line = 1;
        this.col = 1;
        this.errors = new ArrayList<>();
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (true) {
            Token t = nextToken();
            tokens.add(t);
            if (t.type == TokenType.EOF) break;
        }
        return tokens;
    }

    private char current() {
        if (pos >= source.length()) return '\0';
        return source.charAt(pos);
    }

    private char peek(int offset) {
        int idx = pos + offset;
        if (idx >= source.length()) return '\0';
        return source.charAt(idx);
    }

    private char advance() {
        char c = source.charAt(pos++);
        if (c == '\n') { line++; col = 1; }
        else           { col++; }
        return c;
    }

    private void skipWhitespace() {
        while (pos < source.length() && Character.isWhitespace(current())) {
            advance();
        }
    }

    private Token nextToken() {
        skipWhitespace();

        if (pos >= source.length()) {
            return new Token(TokenType.EOF, "EOF", line, col);
        }

        int tokLine = line;
        int tokCol  = col;
        char c = current();

        switch (c) {
            case '[': advance(); return new Token(TokenType.L_CORCHETE, "[", tokLine, tokCol);
            case ']': advance(); return new Token(TokenType.R_CORCHETE, "]", tokLine, tokCol);
            case '{': advance(); return new Token(TokenType.L_LLAVE,    "{", tokLine, tokCol);
            case '}': advance(); return new Token(TokenType.R_LLAVE,    "}", tokLine, tokCol);
            case ',': advance(); return new Token(TokenType.COMA,       ",", tokLine, tokCol);
            case ':': advance(); return new Token(TokenType.DOS_PUNTOS, ":", tokLine, tokCol);
        }

        if (c == '"') {
            return readString(tokLine, tokCol);
        }

        if (Character.isDigit(c) || (c == '-' && Character.isDigit(peek(1)))) {
            return readNumber(tokLine, tokCol);
        }

        if (Character.isLetter(c)) {
            return readKeyword(tokLine, tokCol);
        }

        advance();
        String msg = String.format("Error léxico en L%d:C%d — carácter inesperado '%s'", tokLine, tokCol, c);
        errors.add(msg);
        return new Token(TokenType.ERROR, String.valueOf(c), tokLine, tokCol);
    }

    private Token readString(int tokLine, int tokCol) {
        StringBuilder sb = new StringBuilder();
        sb.append(advance()); 
        while (pos < source.length() && current() != '"') {
            if (current() == '\\') {
                sb.append(advance()); 
                if (pos < source.length()) sb.append(advance()); 
            } else if (current() == '\n') {
                String msg = String.format("Error léxico en L%d:C%d — cadena no terminada", tokLine, tokCol);
                errors.add(msg);
                return new Token(TokenType.ERROR, sb.toString(), tokLine, tokCol);
            } else {
                sb.append(advance());
            }
        }
        if (pos >= source.length()) {
            String msg = String.format("Error léxico en L%d:C%d — cadena no terminada (fin de archivo)", tokLine, tokCol);
            errors.add(msg);
            return new Token(TokenType.ERROR, sb.toString(), tokLine, tokCol);
        }
        sb.append(advance()); 
        return new Token(TokenType.LITERAL_CADENA, sb.toString(), tokLine, tokCol);
    }

    private Token readNumber(int tokLine, int tokCol) {
        StringBuilder sb = new StringBuilder();
        if (current() == '-') sb.append(advance());
        
        while (pos < source.length() && Character.isDigit(current())) {
            sb.append(advance());
        }

        if (current() == '.' && Character.isDigit(peek(1))) {
            sb.append(advance()); // '.'
            while (pos < source.length() && Character.isDigit(current())) {
                sb.append(advance());
            }
        }

        if (current() == 'e' || current() == 'E') {
            sb.append(advance());
            if (current() == '+' || current() == '-') sb.append(advance());
            while (pos < source.length() && Character.isDigit(current())) {
                sb.append(advance());
            }
        }
        return new Token(TokenType.LITERAL_NUM, sb.toString(), tokLine, tokCol);
    }

    private Token readKeyword(int tokLine, int tokCol) {
        StringBuilder sb = new StringBuilder();
        while (pos < source.length() && Character.isLetterOrDigit(current())) {
            sb.append(advance());
        }
        String word = sb.toString();
        switch (word.toLowerCase()) {
            case "true":  return new Token(TokenType.PR_TRUE,  word, tokLine, tokCol);
            case "false": return new Token(TokenType.PR_FALSE, word, tokLine, tokCol);
            case "null":  return new Token(TokenType.PR_NULL,  word, tokLine, tokCol);
            default:
                String msg = String.format("Error léxico en L%d:C%d — identificador desconocido '%s'", tokLine, tokCol, word);
                errors.add(msg);
                return new Token(TokenType.ERROR, word, tokLine, tokCol);
        }
    }
}
