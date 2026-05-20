import java.util.ArrayList;
import java.util.List;

public class Parser {

    private final List<Token> tokens;
    private int pos;
    private final List<String> errors;
    private final StringBuilder xml;
    private int indentLevel;

    private static final TokenType[] SYNC_ELEMENT = {
        TokenType.L_LLAVE, TokenType.L_CORCHETE,
        TokenType.R_LLAVE, TokenType.R_CORCHETE,
        TokenType.COMA, TokenType.EOF
    };

    private static final TokenType[] SYNC_ATTRIBUTE = {
        TokenType.R_LLAVE, TokenType.COMA, TokenType.EOF
    };

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
        this.errors = new ArrayList<>();
        this.xml = new StringBuilder();
        this.indentLevel = 0;
    }

    public List<String> getErrors() { return errors; }
    public String getXml() { return xml.toString(); }

    private Token current() {
        return tokens.get(Math.min(pos, tokens.size() - 1));
    }

    private Token consume() {
        Token t = tokens.get(pos);
        if (t.type != TokenType.EOF) pos++;
        return t;
    }

    private Token expect(TokenType type) {
        Token t = current();
        if (t.type == type) {
            return consume();
        }
        reportError(String.format("Se esperaba '%s' pero se encontró '%s' ('%s')", type, t.type, t.lexeme), t);
        return null;
    }

    private boolean check(TokenType type) {
        return current().type == type;
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (current().type == type) {
                consume();
                return true;
            }
        }
        return false;
    }

    private void reportError(String msg, Token t) {
        errors.add(String.format("Error sintáctico en L%d:C%d — %s", t.line, t.col, msg));
    }

    private void synchronize(TokenType... syncSet) {
        while (current().type != TokenType.EOF) {
            for (TokenType s : syncSet) {
                if (current().type == s) return;
            }
            consume();
        }
    }

    private String indent() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indentLevel; i++) sb.append('\t');
        return sb.toString();
    }

    private void openTag(String name) {
        xml.append(indent()).append('<').append(name).append(">\n");
        indentLevel++;
    }

    private void closeTag(String name) {
        indentLevel--;
        xml.append(indent()).append("</").append(name).append(">\n");
    }

    private void emptyTag(String name) {
        xml.append(indent()).append('<').append(name).append("></").append(name).append(">\n");
    }

    private void leafTag(String name, String value) {
        xml.append(indent()).append('<').append(name).append('>').append(value).append("</").append(name).append(">\n");
    }

    public void parse() {
        parseElement(null);
        expect(TokenType.EOF);
    }

    private void parseElement(String tagName) {
        if (check(TokenType.L_LLAVE)) {
            parseObject(tagName);
        } else if (check(TokenType.L_CORCHETE)) {
            parseArray(tagName);
        } else {
            Token t = current();
            reportError(String.format("Se esperaba '{' o '[' para iniciar elemento, se encontró '%s'", t.lexeme), t);
            synchronize(SYNC_ELEMENT);
        }
    }

    private void parseObject(String tagName) {
        expect(TokenType.L_LLAVE);

        if (tagName != null) {
            openTag(tagName);
        }

        if (check(TokenType.R_LLAVE)) {
            consume();
        } else {
            parseAttributeList();
            if (expect(TokenType.R_LLAVE) == null) {
                synchronize(SYNC_ELEMENT);
            }
        }

        if (tagName != null) {
            closeTag(tagName);
        }
    }

    private void parseAttributeList() {
        parseAttribute();
        while (check(TokenType.COMA)) {
            consume();
            if (check(TokenType.R_LLAVE) || check(TokenType.EOF)) {
                Token t = current();
                reportError("Coma al final de la lista de atributos sin atributo siguiente", t);
                break;
            }
            parseAttribute();
        }
    }

    private void parseAttribute() {
        Token nameToken = current();
        if (!check(TokenType.LITERAL_CADENA)) {
            reportError(String.format("Se esperaba nombre de atributo (cadena), se encontró '%s'", nameToken.lexeme), nameToken);
            synchronize(SYNC_ATTRIBUTE);
            return;
        }
        String rawName = consume().lexeme;

        String tagName = rawName.replaceAll("^\"|\"$", "");

        tagName = sanitizeTagName(tagName);

        if (expect(TokenType.DOS_PUNTOS) == null) {
            synchronize(SYNC_ATTRIBUTE);
            return;
        }

        parseAttributeValue(tagName);
    }

    private void parseAttributeValue(String tagName) {
        Token t = current();

        if (check(TokenType.L_LLAVE) || check(TokenType.L_CORCHETE)) {
            parseElement(tagName);
        } else if (check(TokenType.LITERAL_CADENA)) {
            leafTag(tagName, t.lexeme);
            consume();
        } else if (check(TokenType.LITERAL_NUM)) {
            leafTag(tagName, t.lexeme);
            consume();
        } else if (check(TokenType.PR_TRUE)) {
            leafTag(tagName, "true");
            consume();
        } else if (check(TokenType.PR_FALSE)) {
            leafTag(tagName, "false");
            consume();
        } else if (check(TokenType.PR_NULL)) {
            leafTag(tagName, "null");
            consume();
        } else {
            reportError(String.format("Valor de atributo inválido '%s'", t.lexeme), t);
            synchronize(SYNC_ATTRIBUTE);
        }
    }

    private void parseArray(String tagName) {
        expect(TokenType.L_CORCHETE);

        if (tagName != null) {
            if (check(TokenType.R_CORCHETE)) {
                consume();
                emptyTag(tagName);
                return;
            }
            openTag(tagName);
        }

        if (!check(TokenType.R_CORCHETE) && !check(TokenType.EOF)) {
            parseElementList();
        }

        if (expect(TokenType.R_CORCHETE) == null) {
            synchronize(SYNC_ELEMENT);
        }

        if (tagName != null) {
            closeTag(tagName);
        }
    }

    private void parseElementList() {
        parseArrayItem();
        while (check(TokenType.COMA)) {
            consume(); // ','
            if (check(TokenType.R_CORCHETE) || check(TokenType.EOF)) {
                Token t = current();
                reportError("Coma al final de la lista de elementos sin elemento siguiente", t);
                break;
            }
            parseArrayItem();
        }
    }

    private void parseArrayItem() {
        openTag("item");
        parseElementInner();
        closeTag("item");
    }

    private void parseElementInner() {
        if (check(TokenType.L_LLAVE)) {
            expect(TokenType.L_LLAVE);
            if (!check(TokenType.R_LLAVE) && !check(TokenType.EOF)) {
                parseAttributeList();
            }
            if (expect(TokenType.R_LLAVE) == null) {
                synchronize(SYNC_ELEMENT);
            }
        } else if (check(TokenType.L_CORCHETE)) {
            parseArray(null); 
        } else {
            Token t = current();
            reportError(String.format("Se esperaba '{' o '[', se encontró '%s'", t.lexeme), t);
            synchronize(SYNC_ELEMENT);
        }
    }

    private String sanitizeTagName(String name) {
        String s = name.replaceAll("[^a-zA-Z0-9_\\-.]", "_");
        if (!s.isEmpty() && (Character.isDigit(s.charAt(0)) || s.charAt(0) == '-' || s.charAt(0) == '.')) {
            s = "_" + s;
        }
        return s.isEmpty() ? "_empty" : s;
    }
}
