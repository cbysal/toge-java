package compile.lexical.token;

public class IntLitToken extends Token {
    private final int value;

    IntLitToken(int value) {
        super(TokenType.INT_LIT);
        this.value = value;
    }

    public int second() {
        return value;
    }

    @Override
    public String toString() {
        return String.format("INT %d", value);
    }
}
