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

    public boolean eq(Value v) {
        if (type == Type.FLOAT && v.type == Type.FLOAT)
            return Float.intBitsToFloat(value) == Float.intBitsToFloat(v.value);
        if (type == Type.FLOAT)
            return Float.intBitsToFloat(value) == v.value;
        if (v.type == Type.FLOAT)
            return value == Float.intBitsToFloat(v.value);
        return value == v.value;
    }

    public boolean ge(Value v) {
        if (type == Type.FLOAT && v.type == Type.FLOAT)
            return Float.intBitsToFloat(value) >= Float.intBitsToFloat(v.value);
        if (type == Type.FLOAT)
            return Float.intBitsToFloat(value) >= v.value;
        if (v.type == Type.FLOAT)
            return value >= Float.intBitsToFloat(v.value);
        return value >= v.value;
    }

    public float getFloat() {
        if (type != Type.FLOAT)
            throw new RuntimeException();
        return Float.intBitsToFloat(value);
    }

    public int getInt() {
        if (type != Type.INT)
            throw new RuntimeException();
        return value;
    }

    public boolean gt(Value v) {
        if (type == Type.FLOAT && v.type == Type.FLOAT)
            return Float.intBitsToFloat(value) > Float.intBitsToFloat(v.value);
        if (type == Type.FLOAT)
            return Float.intBitsToFloat(value) > v.value;
        if (v.type == Type.FLOAT)
            return value > Float.intBitsToFloat(v.value);
        return value > v.value;
    }

    public Type getType() {
        return type;
    }

    public boolean le(Value v) {
        if (type == Type.FLOAT && v.type == Type.FLOAT)
            return Float.intBitsToFloat(value) <= Float.intBitsToFloat(v.value);
        if (type == Type.FLOAT)
            return Float.intBitsToFloat(value) <= v.value;
        if (v.type == Type.FLOAT)
            return value <= Float.intBitsToFloat(v.value);
        return value <= v.value;
    }

    public boolean lt(Value v) {
        if (type == Type.FLOAT && v.type == Type.FLOAT)
            return Float.intBitsToFloat(value) < Float.intBitsToFloat(v.value);
        if (type == Type.FLOAT)
            return Float.intBitsToFloat(value) < v.value;
        if (v.type == Type.FLOAT)
            return value < Float.intBitsToFloat(v.value);
        return value < v.value;
    }

    public boolean ne(Value v) {
        if (type == Type.FLOAT && v.type == Type.FLOAT)
            return Float.intBitsToFloat(value) != Float.intBitsToFloat(v.value);
        if (type == Type.FLOAT)
            return Float.intBitsToFloat(value) != v.value;
        if (v.type == Type.FLOAT)
            return value != Float.intBitsToFloat(v.value);
        return value != v.value;
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
