package compile.lexical.token;

public class IntToken extends Token {
    private final int value;

    IntToken(int value) {
        super(TokenType.INT_LIT);
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.format("INT %d", value);
    }
}
