package compile.lexical;

import compile.lexical.token.Token;
import compile.lexical.token.TokenList;
import compile.lexical.token.TokenType;

import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class LexicalParser {
    private record DoubleCharCase(char secondChar, TokenType firstToken, TokenType secondToken) {
    }

    private static final Map<String, TokenType> KEYWORDS = Map.of("break", TokenType.BREAK, "const", TokenType.CONST,
            "continue", TokenType.CONTINUE, "else", TokenType.ELSE, "float", TokenType.FLOAT, "if", TokenType.IF,
            "int", TokenType.INT, "return", TokenType.RETURN, "void", TokenType.VOID, "while", TokenType.WHILE);
    private static final boolean[] OCT_INT_CHARS = new boolean[128];
    private static final boolean[] DEC_INT_CHARS = new boolean[128];
    private static final boolean[] HEX_INT_CHARS = new boolean[128];
    private static final boolean[] FLOAT_CHARS = new boolean[128];
    private static final boolean[] ID_CHARS = new boolean[128];
    private static final Pattern DEC_INT_PATTERN = Pattern.compile("[1-9]\\d*");
    private static final Pattern FLOAT_PATTERN = Pattern.compile("(\\d+\\.\\d*|\\.\\d+)([Ee][+-]?\\d+)?|\\d+[Ee][" +
            "+-]?\\d+|0[Xx]([\\dA-Fa-f]+\\.[\\dA-Fa-f]*|\\.[\\dA-Fa-f]+)[Pp][+-]?\\d+");
    private static final Pattern ID_PATTERN = Pattern.compile("[A-Za-z_][\\dA-Za-z_]*");
    private static final Pattern HEX_INT_PATTERN = Pattern.compile("0[Xx][\\d|A-Fa-f]+");
    private static final Pattern OCT_INT_PATTERN = Pattern.compile("0[0-7]*");
    private static final Map<Character, TokenType> SINGLE_CHAR_CASES = Map.ofEntries(Map.entry('%', TokenType.MOD),
            Map.entry('(', TokenType.LP), Map.entry(')', TokenType.RP), Map.entry('*', TokenType.MUL), Map.entry('+',
                    TokenType.PLUS), Map.entry(',', TokenType.COMMA), Map.entry('-', TokenType.MINUS), Map.entry('/',
                    TokenType.DIV), Map.entry(';', TokenType.SEMICOLON), Map.entry('[', TokenType.LB), Map.entry(']',
                    TokenType.RB), Map.entry('{', TokenType.LC), Map.entry('}', TokenType.RC));
    private static final Map<Character, DoubleCharCase> DOUBLE_CHAR_CASES = Map.of('!', new DoubleCharCase('=',
            TokenType.L_NOT, TokenType.NE), '&', new DoubleCharCase('&', null, TokenType.L_AND), '<',
            new DoubleCharCase('=', TokenType.LT, TokenType.LE), '=', new DoubleCharCase('=', TokenType.ASSIGN,
                    TokenType.EQ), '>', new DoubleCharCase('=', TokenType.GT, TokenType.GE), '|',
            new DoubleCharCase('|', null, TokenType.L_OR));

    static {
        for (char c = '0'; c <= '9'; c++) {
            if (c < '8') {
                OCT_INT_CHARS[c] = true;
            }
            DEC_INT_CHARS[c] = true;
            HEX_INT_CHARS[c] = true;
            FLOAT_CHARS[c] = true;
            ID_CHARS[c] = true;
        }
        for (char c = 'A'; c <= 'Z'; c++) {
            if (c <= 'F') {
                HEX_INT_CHARS[c] = true;
                FLOAT_CHARS[c] = true;
            }
            ID_CHARS[c] = true;
        }
        for (char c = 'a'; c <= 'z'; c++) {
            if (c <= 'f') {
                HEX_INT_CHARS[c] = true;
                FLOAT_CHARS[c] = true;
            }
            ID_CHARS[c] = true;
        }
        HEX_INT_CHARS['X'] = true;
        HEX_INT_CHARS['x'] = true;
        FLOAT_CHARS['P'] = true;
        FLOAT_CHARS['p'] = true;
        FLOAT_CHARS['X'] = true;
        FLOAT_CHARS['x'] = true;
        FLOAT_CHARS['.'] = true;
        FLOAT_CHARS['+'] = true;
        FLOAT_CHARS['-'] = true;
        ID_CHARS['_'] = true;
    }

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
        head = skipChars(head, Character::isWhitespace);
        if (head == content.length()) {
            return null;
        }
        if (SINGLE_CHAR_CASES.containsKey(content.charAt(head))) {
            return matchSingleCharToken(SINGLE_CHAR_CASES.get(content.charAt(head)));
        }
        if (DOUBLE_CHAR_CASES.containsKey(content.charAt(head))) {
            DoubleCharCase doubleCharCase = DOUBLE_CHAR_CASES.get(content.charAt(head));
            return matchDoubleCharToken(doubleCharCase.secondChar(), doubleCharCase.firstToken(),
                    doubleCharCase.secondToken());
        }
        return matchMultiCharToken();
    }

    private Token matchSingleCharToken(TokenType type) {
        head++;
        return Token.valueOf(type);
    }

    private Token matchDoubleCharToken(char secondChar, TokenType singleCharToken, TokenType doubleCharToken) {
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
        int tail = skipChars(head, c -> ID_CHARS[c]);
        String matched = content.substring(head, tail);
        if (!ID_PATTERN.matcher(matched).matches()) {
            return null;
        }
        head = tail;
        if (KEYWORDS.containsKey(matched)) {
            return Token.valueOf(KEYWORDS.get(matched));
        }
        return Token.valueOf(matched);
    }

    private Token tryMatchFloat() {
        int tail = skipChars(head, c -> FLOAT_CHARS[c]);
        String matched = content.substring(head, tail);
        if (!FLOAT_PATTERN.matcher(matched).matches()) {
            return null;
        }
        head = tail;
        return Token.valueOf(Float.parseFloat(matched));
    }

    private Token tryMatchHexInt() {
        int tail = skipChars(head, c -> HEX_INT_CHARS[c]);
        String matched = content.substring(head, tail);
        if (!HEX_INT_PATTERN.matcher(matched).matches()) {
            return null;
        }
        head = tail;
        return Token.valueOf(Integer.parseInt(matched, 2, matched.length(), 16));
    }

    private Token tryMatchDecInt() {
        int tail = skipChars(head, c -> DEC_INT_CHARS[c]);
        String matched = content.substring(head, tail);
        if (!DEC_INT_PATTERN.matcher(matched).matches()) {
            return null;
        }
        head = tail;
        return Token.valueOf(Integer.parseInt(matched));
    }

    private Token tryMatchOctInt() {
        int tail = skipChars(head, c -> OCT_INT_CHARS[c]);
        String matched = content.substring(head, tail);
        if (!OCT_INT_PATTERN.matcher(matched).matches()) {
            return null;
        }
        head = tail;
        // TODO here's a bug, but with this bug, program runs, fix it later
        // This branch matches decimal number 0, but it should not appear here.
        // With the following method call, a octal number string will also treat leading 0 as the digit in the octal
        // number. e.g. 012, that is 1 * 8 + 2 = 10, but with the following method call, it will be 0 * 64 + 1 * 8 + 2
        // = 10. That is wrong in logic, but is right in result.
        return Token.valueOf(Integer.parseInt(matched, 8));
    }

    private int skipChars(int index, Predicate<Character> predicate) {
        while (index < content.length() && predicate.test(content.charAt(index))) {
            index++;
        }
        return index;
    }
}
