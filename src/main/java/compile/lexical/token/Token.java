package compile.lexical.token;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class Token {
    private static final Map<TokenType, Token> TOKEN_POOL =
            Arrays.stream(TokenType.values()).collect(Collectors.toMap(type -> type, Token::new));

    private final TokenType type;

    Token(TokenType type) {
        this.type = type;
    }

    public static Token valueOf(float value) {
        return new FloatToken(value);
    }

    public static Token valueOf(int value) {
        return new IntToken(value);
    }

    public static Token valueOf(String id) {
        return new IdToken(id);
    }

    public static Token valueOf(TokenType type) {
        return TOKEN_POOL.get(type);
    }

    public TokenType getType() {
        return type;
    }

    @Override
    public String toString() {
        return type.toString();
    }
}
