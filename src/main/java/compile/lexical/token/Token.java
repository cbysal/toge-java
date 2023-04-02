package compile.lexical.token;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class Token {
    public enum Type {
        ASSIGN, BREAK, COMMA, CONST, CONTINUE, DIV, ELSE, EQ, FLOAT, FLOAT_LIT, GE, GT, ID, IF, INT, INT_LIT, LB, LC,
        LE, LT, L_AND, L_NOT, L_OR, LP, MINUS, MOD, MUL, NE, PLUS, RP, RB, RC, RETURN, SEMICOLON, VOID, WHILE
    }

    private static final Map<Type, Token> TOKEN_POOL =
            Arrays.stream(Type.values()).collect(Collectors.toMap(type -> type, Token::new));

    private final Type type;

    Token(Type type) {
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

    public static Token valueOf(Type type) {
        return TOKEN_POOL.get(type);
    }

    @Override
    public String toString() {
        return type.toString();
    }
}
