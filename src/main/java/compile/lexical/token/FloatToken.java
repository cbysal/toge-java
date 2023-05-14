package compile.lexical.token;

public class FloatToken extends Token {
    private final float value;

    FloatToken(float value) {
        super(TokenType.FLOAT_LIT);
        this.value = value;
    }

    public float getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.format("FLOAT %f", value);
    }
}
