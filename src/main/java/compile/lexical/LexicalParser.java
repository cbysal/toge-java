package compile.lexical;

import compile.lexical.token.Token;
import compile.lexical.token.TokenList;
import compile.lexical.token.TokenType;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class LexicalParser {
    private static final Map<String, TokenType> KEYWORDS = Map.of("break", TokenType.BREAK, "const", TokenType.CONST,
            "continue", TokenType.CONTINUE, "else", TokenType.ELSE, "float", TokenType.FLOAT, "if", TokenType.IF,
            "int", TokenType.INT, "return", TokenType.RETURN, "void", TokenType.VOID, "while", TokenType.WHILE);
    private static final Set<Character> DECIMAL_INT_CHARSET = Set.of('0', '1', '2', '3', '4', '5', '6', '7', '8', '9');
    private static final Pattern DECIMAL_INT_PATTERN = Pattern.compile("[1-9]\\d*");
    private static final Set<Character> FLOAT_CHARSET = Set.of('+', '-', '.', '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'P', 'X', 'a', 'b', 'c', 'd', 'e', 'f', 'p', 'x');
    private static final Pattern FLOAT_PATTERN = Pattern.compile("(\\d+\\.\\d*|\\.\\d+)([Ee][+-]?\\d+)?|\\d+[Ee][" +
            "+-]?\\d+|0[Xx]([\\dA-Fa-f]+\\.[\\dA-Fa-f]*|\\.[\\dA-Fa-f]+)[Pp][+-]?\\d+");
    private static final Set<Character> IDENTITY_CHARSET = Set.of('0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U',
            'V', 'W', 'X', 'Y', 'Z', '_', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o',
            'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z');
    private static final Pattern IDENTITY_PATTERN = Pattern.compile("[A-Za-z_][\\dA-Za-z_]*");
    private static final Set<Character> HEXADECIMAL_INT_CHARSET = Set.of('0', '1', '2', '3', '4', '5', '6', '7', '8',
            '9', 'A', 'B', 'C', 'D', 'E', 'F', 'X', 'a', 'b', 'c', 'd', 'e', 'f', 'x');
    private static final Pattern HEXADECIMAL_INT_PATTERN = Pattern.compile("0[Xx][\\d|A-Fa-f]+");
    private static final Set<Character> OCTAL_INT_CHARSET = Set.of('0', '1', '2', '3', '4', '5', '6', '7');
    private static final Pattern OCTAL_INT_PATTERN = Pattern.compile("0[0-7]*");
    private final String content;
    private final TokenList tokens = new TokenList();
    private int head = 0;

    public LexicalParser(String content) {
        this.content = content;
    }

    public TokenList getTokens() {
        Token token;
        while ((token = nextToken()) != null)
            tokens.add(token);
        return tokens;
    }

    private Token nextToken() {
        while (head < content.length() && Character.isWhitespace(content.charAt(head)))
            head++;
        if (head == content.length())
            return null;
        return switch (content.charAt(head)) {
            case '!' -> {
                if (head + 1 < content.length() && content.charAt(head + 1) == '=') {
                    head += 2;
                    yield Token.valueOf(TokenType.NE);
                } else {
                    head++;
                    yield Token.valueOf(TokenType.L_NOT);
                }
            }
            case '%' -> {
                head++;
                yield Token.valueOf(TokenType.MOD);
            }
            case '&' -> {
                if (head + 1 < content.length() && content.charAt(head + 1) == '&') {
                    head += 2;
                    yield Token.valueOf(TokenType.L_AND);
                } else
                    throw new RuntimeException("It should be token AND, but has not implemented now!");
            }
            case '(' -> {
                head++;
                yield Token.valueOf(TokenType.LP);
            }
            case ')' -> {
                head++;
                yield Token.valueOf(TokenType.RP);
            }
            case '*' -> {
                head++;
                yield Token.valueOf(TokenType.MUL);
            }
            case '+' -> {
                head++;
                yield Token.valueOf(TokenType.PLUS);
            }
            case ',' -> {
                head++;
                yield Token.valueOf(TokenType.COMMA);
            }
            case '-' -> {
                head++;
                yield Token.valueOf(TokenType.MINUS);
            }
            case '/' -> {
                head++;
                yield Token.valueOf(TokenType.DIV);
            }
            case ';' -> {
                head++;
                yield Token.valueOf(TokenType.SEMICOLON);
            }
            case '<' -> {
                if (head + 1 < content.length() && content.charAt(head + 1) == '=') {
                    head += 2;
                    yield Token.valueOf(TokenType.LE);
                } else {
                    head++;
                    yield Token.valueOf(TokenType.LT);
                }
            }
            case '=' -> {
                if (head + 1 < content.length() && content.charAt(head + 1) == '=') {
                    head += 2;
                    yield Token.valueOf(TokenType.EQ);
                } else {
                    head++;
                    yield Token.valueOf(TokenType.ASSIGN);
                }
            }
            case '>' -> {
                if (content.charAt(head + 1) == '=') {
                    head += 2;
                    yield Token.valueOf(TokenType.GE);
                } else {
                    head++;
                    yield Token.valueOf(TokenType.GT);
                }
            }
            case '[' -> {
                head++;
                yield Token.valueOf(TokenType.LB);
            }
            case ']' -> {
                head++;
                yield Token.valueOf(TokenType.RB);
            }
            case '{' -> {
                head++;
                yield Token.valueOf(TokenType.LC);
            }
            case '|' -> {
                if (head + 1 < content.length() && content.charAt(head + 1) == '|') {
                    head += 2;
                    yield Token.valueOf(TokenType.L_OR);
                } else
                    throw new RuntimeException("It should be token OR, but has not implemented now!");
            }
            case '}' -> {
                head++;
                yield Token.valueOf(TokenType.RC);
            }
            default -> {
                Token tryToken;
                tryToken = tryMatchKeywordOrIdentity();
                if (tryToken != null)
                    yield tryToken;
                tryToken = tryMatchFloat();
                if (tryToken != null)
                    yield tryToken;
                tryToken = tryMatchHexInt();
                if (tryToken != null)
                    yield tryToken;
                tryToken = tryMatchDecInt();
                if (tryToken != null)
                    yield tryToken;
                tryToken = tryMatchOctInt();
                if (tryToken != null)
                    yield tryToken;
                throw new RuntimeException("Can not match at position: " + head);
            }
        };
    }

    private Token tryMatchDecInt() {
        int tail = head;
        while (DECIMAL_INT_CHARSET.contains(content.charAt(tail)))
            tail++;
        String tokenStr = content.substring(head, tail);
        if (DECIMAL_INT_PATTERN.matcher(tokenStr).matches()) {
            head = tail;
            return Token.valueOf(Integer.parseInt(tokenStr));
        }
        return null;
    }

    private Token tryMatchFloat() {
        int tail = head;
        while (FLOAT_CHARSET.contains(content.charAt(tail)))
            tail++;
        String tokenStr = content.substring(head, tail);
        if (FLOAT_PATTERN.matcher(tokenStr).matches()) {
            head = tail;
            return Token.valueOf(Float.parseFloat(tokenStr));
        }
        return null;
    }

    private Token tryMatchHexInt() {
        int tail = head;
        while (HEXADECIMAL_INT_CHARSET.contains(content.charAt(tail)))
            tail++;
        String tokenStr = content.substring(head, tail);
        if (HEXADECIMAL_INT_PATTERN.matcher(tokenStr).matches()) {
            head = tail;
            return Token.valueOf(Integer.parseInt(tokenStr.substring(2), 16));
        }
        return null;
    }

    private Token tryMatchKeywordOrIdentity() {
        int tail = head;
        while (IDENTITY_CHARSET.contains(content.charAt(tail)))
            tail++;
        String tokenStr = content.substring(head, tail);
        if (IDENTITY_PATTERN.matcher(tokenStr).matches()) {
            head = tail;
            if (KEYWORDS.containsKey(tokenStr))
                return Token.valueOf(KEYWORDS.get(tokenStr));
            return Token.valueOf(tokenStr);
        }
        return null;
    }

    private Token tryMatchOctInt() {
        int tail = head;
        while (OCTAL_INT_CHARSET.contains(content.charAt(tail)))
            tail++;
        String tokenStr = content.substring(head, tail);
        if (OCTAL_INT_PATTERN.matcher(tokenStr).matches()) {
            head = tail;
            return Token.valueOf(Integer.parseInt(tokenStr, 8));
        }
        return null;
    }
}
