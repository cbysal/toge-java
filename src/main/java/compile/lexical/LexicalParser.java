package compile.lexical;

import compile.lexical.token.Token;
import compile.lexical.token.TokenList;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LexicalParser {
    private static final Map<String, Token.Type> KEYWORDS = Map.of("break", Token.Type.BREAK, "const",
            Token.Type.CONST, "continue", Token.Type.CONTINUE, "else", Token.Type.ELSE, "float", Token.Type.FLOAT,
            "if", Token.Type.IF, "int", Token.Type.INT, "return", Token.Type.RETURN, "void", Token.Type.VOID, "while"
            , Token.Type.WHILE);
    private static final Pattern DEC_INT_PATTERN = Pattern.compile("[1-9]\\d*");
    private static final Pattern FLOAT_PATTERN = Pattern.compile("(\\d+\\.\\d*|\\.\\d+)([Ee][+-]?\\d+)?|\\d+[Ee][" +
            "+-]?\\d+|0[Xx]([\\dA-Fa-f]+\\.[\\dA-Fa-f]*|\\.[\\dA-Fa-f]+)[Pp][+-]?\\d+");
    private static final Pattern ID_PATTERN = Pattern.compile("[A-Za-z_][\\dA-Za-z_]*");
    private static final Pattern HEX_INT_PATTERN = Pattern.compile("0[Xx][\\d|A-Fa-f]+");
    private static final Pattern OCT_INT_PATTERN = Pattern.compile("0[0-7]*");

    private boolean isProcessed;
    private int head;
    private final String content;
    private final TokenList tokens = new TokenList();

    public LexicalParser(String content) {
        this.content = content;
    }

    private void checkIfIsProcessed() {
        if (isProcessed) {
            return;
        }
        isProcessed = true;
        Token token;
        while ((token = nextToken()) != null) {
            tokens.add(token);
        }
    }

    public TokenList getTokens() {
        checkIfIsProcessed();
        return tokens;
    }

    private Token nextToken() {
        skipWhilespace();
        if (head == content.length()) {
            return null;
        }
        return switch (content.charAt(head)) {
            case '!' -> matchDoubleCharToken('=', Token.Type.L_NOT, Token.Type.NE);
            case '%' -> matchSingleCharToken(Token.Type.MOD);
            case '&' -> matchDoubleCharToken('&', null, Token.Type.L_AND);
            case '(' -> matchSingleCharToken(Token.Type.LP);
            case ')' -> matchSingleCharToken(Token.Type.RP);
            case '*' -> matchSingleCharToken(Token.Type.MUL);
            case '+' -> matchSingleCharToken(Token.Type.PLUS);
            case ',' -> matchSingleCharToken(Token.Type.COMMA);
            case '-' -> matchSingleCharToken(Token.Type.MINUS);
            case '/' -> matchSingleCharToken(Token.Type.DIV);
            case ';' -> matchSingleCharToken(Token.Type.SEMICOLON);
            case '<' -> matchDoubleCharToken('=', Token.Type.LT, Token.Type.LE);
            case '=' -> matchDoubleCharToken('=', Token.Type.ASSIGN, Token.Type.EQ);
            case '>' -> matchDoubleCharToken('=', Token.Type.GT, Token.Type.GE);
            case '[' -> matchSingleCharToken(Token.Type.LB);
            case ']' -> matchSingleCharToken(Token.Type.RB);
            case '{' -> matchSingleCharToken(Token.Type.LC);
            case '|' -> matchDoubleCharToken('|', null, Token.Type.L_OR);
            case '}' -> matchSingleCharToken(Token.Type.RC);
            default -> matchMultiCharToken();
        };
    }

    private void skipWhilespace() {
        while (head < content.length() && Character.isWhitespace(content.charAt(head))) {
            head++;
        }
    }

    private Token matchSingleCharToken(Token.Type type) {
        head++;
        return Token.valueOf(type);
    }

    private Token matchDoubleCharToken(char secondChar, Token.Type singleCharToken, Token.Type doubleCharToken) {
        if (head + 1 < content.length() && content.charAt(head + 1) == secondChar) {
            head += 2;
            return Token.valueOf(doubleCharToken);
        }
        if (singleCharToken == null) {
            throw new RuntimeException("Can not continue matching at position " + head);
        }
        head++;
        return Token.valueOf(singleCharToken);
    }

    private Token matchMultiCharToken() {
        Token tryToken;
        tryToken = tryMatchKeywordOrId();
        if (tryToken != null) {
            return tryToken;
        }
        tryToken = tryMatchFloat();
        if (tryToken != null) {
            return tryToken;
        }
        tryToken = tryMatchHexInt();
        if (tryToken != null) {
            return tryToken;
        }
        tryToken = tryMatchDecInt();
        if (tryToken != null) {
            return tryToken;
        }
        tryToken = tryMatchOctInt();
        if (tryToken != null) {
            return tryToken;
        }
        throw new RuntimeException("Can not continue matching at position " + head);
    }

    private Token tryMatchKeywordOrId() {
        Matcher matcher = ID_PATTERN.matcher(content).region(head, content.length());
        if (!matcher.find() || matcher.start() != head) {
            return null;
        }
        head = matcher.end();
        String matched = matcher.group();
        if (KEYWORDS.containsKey(matched)) {
            return Token.valueOf(KEYWORDS.get(matched));
        }
        return Token.valueOf(matched);
    }

    private Token tryMatchFloat() {
        Matcher matcher = FLOAT_PATTERN.matcher(content).region(head, content.length());
        if (!matcher.find() || matcher.start() != head) {
            return null;
        }
        head = matcher.end();
        String matched = matcher.group();
        return Token.valueOf(Float.parseFloat(matched));
    }

    private Token tryMatchHexInt() {
        Matcher matcher = HEX_INT_PATTERN.matcher(content).region(head, content.length());
        if (!matcher.find() || matcher.start() != head) {
            return null;
        }
        head = matcher.end();
        String matched = matcher.group();
        return Token.valueOf(Integer.parseInt(matched, 2, matched.length(), 16));
    }

    private Token tryMatchDecInt() {
        Matcher matcher = DEC_INT_PATTERN.matcher(content).region(head, content.length());
        if (!matcher.find() || matcher.start() != head) {
            return null;
        }
        head = matcher.end();
        String matched = matcher.group();
        return Token.valueOf(Integer.parseInt(matched));
    }

    private Token tryMatchOctInt() {
        Matcher matcher = OCT_INT_PATTERN.matcher(content).region(head, content.length());
        if (!matcher.find() || matcher.start() != head) {
            return null;
        }
        head = matcher.end();
        String matched = matcher.group();
        return Token.valueOf(Integer.parseInt(matched, 1, matched.length(), 8));
    }
}
