package compile.symbol;

import compile.codegen.virgen.vir.VIRItem;
import compile.codegen.virgen.vir.type.BasicType;
import compile.codegen.virgen.vir.type.Type;

import java.util.Objects;

public final class Value implements VIRItem {
    private final Number value;

    public Value(boolean value) {
        this.value = value ? 1 : 0;
    }

    public Value(Number value) {
        this.value = value;
    }

    public Value add(Value v) {
        if (value instanceof Float || v.value instanceof Float)
            return new Value(value.floatValue() + v.value.floatValue());
        return new Value(value.intValue() + v.value.intValue());
    }

    public Value sub(Value v) {
        if (value instanceof Float || v.value instanceof Float)
            return new Value(value.floatValue() - v.value.floatValue());
        return new Value(value.intValue() - v.value.intValue());
    }

    public Value mul(Value v) {
        if (value instanceof Float || v.value instanceof Float)
            return new Value(value.floatValue() * v.value.floatValue());
        return new Value(value.intValue() * v.value.intValue());
    }

    public Value div(Value v) {
        if (value instanceof Float || v.value instanceof Float)
            return new Value(value.floatValue() / v.value.floatValue());
        return new Value(value.intValue() / v.value.intValue());
    }

    public Value mod(Value v) {
        if (value instanceof Float || v.value instanceof Float)
            throw new RuntimeException();
        return new Value(value.intValue() % v.value.intValue());
    }

    public Value eq(Value v) {
        if (value instanceof Float || v.value instanceof Float)
            return new Value(value.floatValue() == v.value.floatValue());
        return new Value(value.intValue() == v.value.intValue());
    }

    public Value ne(Value v) {
        if (value instanceof Float || v.value instanceof Float)
            return new Value(value.floatValue() != v.value.floatValue());
        return new Value(value.intValue() != v.value.intValue());
    }

    public Value ge(Value v) {
        if (value instanceof Float || v.value instanceof Float)
            return new Value(value.floatValue() >= v.value.floatValue());
        return new Value(value.intValue() >= v.value.intValue());
    }

    public Value gt(Value v) {
        if (value instanceof Float || v.value instanceof Float)
            return new Value(value.floatValue() > v.value.floatValue());
        return new Value(value.intValue() > v.value.intValue());
    }

    public Value le(Value v) {
        if (value instanceof Float || v.value instanceof Float)
            return new Value(value.floatValue() <= v.value.floatValue());
        return new Value(value.intValue() <= v.value.intValue());
    }

    public Value lt(Value v) {
        if (value instanceof Float || v.value instanceof Float)
            return new Value(value.floatValue() < v.value.floatValue());
        return new Value(value.intValue() < v.value.intValue());
    }

    public Value neg() {
        if (value instanceof Float)
            return new Value(-value.floatValue());
        return new Value(-value.intValue());
    }

    public Value lNot() {
        if (value instanceof Float)
            return new Value(value.floatValue() == 0.0f);
        return new Value(value.intValue() == 0);
    }

    public Value abs() {
        if (value instanceof Float)
            return new Value(Math.abs(value.floatValue()));
        return new Value(Math.abs(value.intValue()));
    }

    public Value toInt() {
        return new Value(value.intValue());
    }

    public Value toFloat() {
        return new Value(value.floatValue());
    }

    public boolean isZero() {
        if (value instanceof Float)
            return value.floatValue() == 0.0f;
        return value.intValue() == 0;
    }

    public Type getType() {
        if (value instanceof Float)
            return BasicType.FLOAT;
        return BasicType.I32;
    }

    public int intValue() {
        return value.intValue();
    }

    public float floatValue() {
        return value.floatValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Value value1 = (Value) o;
        return Objects.equals(value, value1.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "#" + value;
    }
}
