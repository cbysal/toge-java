package compile.symbol;

import compile.codegen.virgen.vir.VIRItem;

import java.util.Objects;

public final class Value implements VIRItem {
    private final Type type;
    private final int value;

    public Value(boolean value) {
        this.type = Type.INT;
        this.value = value ? 1 : 0;
    }

    public Value(float value) {
        this.type = Type.FLOAT;
        this.value = Float.floatToIntBits(value);
    }

    public Value(int value) {
        this.type = Type.INT;
        this.value = value;
    }

    public Value(Number value) {
        if (value instanceof Integer) {
            this.type = Type.INT;
            this.value = (int) value;
        } else if (value instanceof Float) {
            this.type = Type.FLOAT;
            this.value = Float.floatToIntBits(value.floatValue());
        } else
            throw new RuntimeException();
    }

    public Value add(Value v) {
        if (type == Type.FLOAT && v.type == Type.FLOAT)
            return new Value(Float.intBitsToFloat(value) + Float.intBitsToFloat(v.value));
        if (type == Type.FLOAT)
            return new Value(Float.intBitsToFloat(value) + v.value);
        if (v.type == Type.FLOAT)
            return new Value(value + Float.intBitsToFloat(v.value));
        return new Value(value + v.value);
    }

    public Value sub(Value v) {
        if (type == Type.FLOAT && v.type == Type.FLOAT)
            return new Value(Float.intBitsToFloat(value) - Float.intBitsToFloat(v.value));
        if (type == Type.FLOAT)
            return new Value(Float.intBitsToFloat(value) - v.value);
        if (v.type == Type.FLOAT)
            return new Value(value - Float.intBitsToFloat(v.value));
        return new Value(value - v.value);
    }

    public Value mul(Value v) {
        if (type == Type.FLOAT && v.type == Type.FLOAT)
            return new Value(Float.intBitsToFloat(value) * Float.intBitsToFloat(v.value));
        if (type == Type.FLOAT)
            return new Value(Float.intBitsToFloat(value) * v.value);
        if (v.type == Type.FLOAT)
            return new Value(value * Float.intBitsToFloat(v.value));
        return new Value(value * v.value);
    }

    public Value div(Value v) {
        if (type == Type.FLOAT && v.type == Type.FLOAT)
            return new Value(Float.intBitsToFloat(value) / Float.intBitsToFloat(v.value));
        if (type == Type.FLOAT)
            return new Value(Float.intBitsToFloat(value) / v.value);
        if (v.type == Type.FLOAT)
            return new Value(value / Float.intBitsToFloat(v.value));
        return new Value(value / v.value);
    }

    public Value mod(Value v) {
        return new Value(value % v.value);
    }

    public Value eq(Value v) {
        if (type == Type.FLOAT && v.type == Type.FLOAT)
            return new Value(Float.intBitsToFloat(value) == Float.intBitsToFloat(v.value));
        if (type == Type.FLOAT)
            return new Value(Float.intBitsToFloat(value) == v.value);
        if (v.type == Type.FLOAT)
            return new Value(value == Float.intBitsToFloat(v.value));
        return new Value(value == v.value);
    }

    public Value ne(Value v) {
        if (type == Type.FLOAT && v.type == Type.FLOAT)
            return new Value(Float.intBitsToFloat(value) != Float.intBitsToFloat(v.value));
        if (type == Type.FLOAT)
            return new Value(Float.intBitsToFloat(value) != v.value);
        if (v.type == Type.FLOAT)
            return new Value(value != Float.intBitsToFloat(v.value));
        return new Value(value != v.value);
    }

    public Value ge(Value v) {
        if (type == Type.FLOAT && v.type == Type.FLOAT)
            return new Value(Float.intBitsToFloat(value) >= Float.intBitsToFloat(v.value));
        if (type == Type.FLOAT)
            return new Value(Float.intBitsToFloat(value) >= v.value);
        if (v.type == Type.FLOAT)
            return new Value(value >= Float.intBitsToFloat(v.value));
        return new Value(value >= v.value);
    }

    public Value gt(Value v) {
        if (type == Type.FLOAT && v.type == Type.FLOAT)
            return new Value(Float.intBitsToFloat(value) > Float.intBitsToFloat(v.value));
        if (type == Type.FLOAT)
            return new Value(Float.intBitsToFloat(value) > v.value);
        if (v.type == Type.FLOAT)
            return new Value(value > Float.intBitsToFloat(v.value));
        return new Value(value > v.value);
    }

    public Value toInt() {
        if (type == Type.INT)
            throw new RuntimeException();
        return new Value((int) Float.intBitsToFloat(value));
    }

    public Value toFloat() {
        if (type == Type.FLOAT)
            throw new RuntimeException();
        return new Value((float) value);
    }

    public Value neg() {
        if (type == Type.FLOAT)
            return new Value(-Float.intBitsToFloat(value));
        return new Value(-value);
    }

    public Value lNot() {
        if (type == Type.FLOAT)
            return new Value(Float.intBitsToFloat(value) == 0.0f);
        return new Value(value == 0);
    }

    public boolean isZero() {
        if (type == Type.FLOAT)
            return Float.intBitsToFloat(value) == 0.0f;
        return value == 0;
    }

    public Value abs() {
        if (type == Type.FLOAT)
            return new Value(Math.abs(Float.intBitsToFloat(value)));
        return new Value(Math.abs(value));
    }

    public Value le(Value v) {
        if (type == Type.FLOAT && v.type == Type.FLOAT)
            return new Value(Float.intBitsToFloat(value) <= Float.intBitsToFloat(v.value));
        if (type == Type.FLOAT)
            return new Value(Float.intBitsToFloat(value) <= v.value);
        if (v.type == Type.FLOAT)
            return new Value(value <= Float.intBitsToFloat(v.value));
        return new Value(value <= v.value);
    }

    public Value lt(Value v) {
        if (type == Type.FLOAT && v.type == Type.FLOAT)
            return new Value(Float.intBitsToFloat(value) < Float.intBitsToFloat(v.value));
        if (type == Type.FLOAT)
            return new Value(Float.intBitsToFloat(value) < v.value);
        if (v.type == Type.FLOAT)
            return new Value(value < Float.intBitsToFloat(v.value));
        return new Value(value < v.value);
    }

    public Type getType() {
        return type;
    }

    public float getFloat() {
        if (type == Type.INT)
            return value;
        return Float.intBitsToFloat(value);
    }

    public int getInt() {
        if (type == Type.INT)
            return value;
        return (int) Float.intBitsToFloat(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Value value1 = (Value) o;
        return value == value1.value && type == value1.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value);
    }

    @Override
    public String toString() {
        return "#" + (type == Type.FLOAT ? String.valueOf(Float.intBitsToFloat(value)) : String.valueOf(value));
    }
}
