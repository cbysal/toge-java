package compile.symbol;

public final class Value {
    private final boolean isFloat;
    private final int value;

    public Value(boolean value) {
        this.isFloat = false;
        this.value = value ? 1 : 0;
    }

    public Value(float value) {
        this.isFloat = true;
        this.value = Float.floatToIntBits(value);
    }

    public Value(int value) {
        this.isFloat = false;
        this.value = value;
    }

    public float getFloat() {
        if (!isFloat) {
            throw new RuntimeException();
        }
        return Float.intBitsToFloat(value);
    }

    public int getInt() {
        if (isFloat) {
            throw new RuntimeException();
        }
        return value;
    }

    public boolean isFloat() {
        return isFloat;
    }

    @Override
    public String toString() {
        return "#" + (isFloat ? String.valueOf(Float.intBitsToFloat(value)) : String.valueOf(value));
    }
}
