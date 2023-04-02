package compile.lexical.token;

public class IntToken extends Token {
    private final int value;

    IntToken(int value) {
        super(Type.INT_LIT);
        this.value = value;
    }

    public int getInt() {
        return value;
    }

    @Override
    public String toString() {
        return "INT " + value;
    }
}
