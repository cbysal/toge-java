package compile.lexical.token;

public class FloatToken extends Token {
    private final float value;

    FloatToken(float value) {
        super(Type.FLOAT_LIT);
        this.value = value;
    }

    public float getFloat() {
        return value;
    }

    @Override
    public String toString() {
        return "FLOAT " + value;
    }
}
