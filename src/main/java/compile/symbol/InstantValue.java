package compile.symbol;

import compile.vir.ir.VIRItem;
import compile.vir.type.BasicType;
import compile.vir.type.Type;

import java.util.Objects;

public final class InstantValue implements VIRItem {
    private final Number value;

    public InstantValue(boolean value) {
        this.value = value ? 1 : 0;
    }

    public InstantValue(Number value) {
        this.value = value;
    }

    public InstantValue add(InstantValue v) {
        if (value instanceof Float || v.value instanceof Float)
            return new InstantValue(value.floatValue() + v.value.floatValue());
        return new InstantValue(value.intValue() + v.value.intValue());
    }

    public InstantValue sub(InstantValue v) {
        if (value instanceof Float || v.value instanceof Float)
            return new InstantValue(value.floatValue() - v.value.floatValue());
        return new InstantValue(value.intValue() - v.value.intValue());
    }

    public InstantValue mul(InstantValue v) {
        if (value instanceof Float || v.value instanceof Float)
            return new InstantValue(value.floatValue() * v.value.floatValue());
        return new InstantValue(value.intValue() * v.value.intValue());
    }

    public InstantValue div(InstantValue v) {
        if (value instanceof Float || v.value instanceof Float)
            return new InstantValue(value.floatValue() / v.value.floatValue());
        return new InstantValue(value.intValue() / v.value.intValue());
    }

    public InstantValue mod(InstantValue v) {
        if (value instanceof Float || v.value instanceof Float)
            throw new RuntimeException();
        return new InstantValue(value.intValue() % v.value.intValue());
    }

    public InstantValue eq(InstantValue v) {
        if (value instanceof Float || v.value instanceof Float)
            return new InstantValue(value.floatValue() == v.value.floatValue());
        return new InstantValue(value.intValue() == v.value.intValue());
    }

    public InstantValue ne(InstantValue v) {
        if (value instanceof Float || v.value instanceof Float)
            return new InstantValue(value.floatValue() != v.value.floatValue());
        return new InstantValue(value.intValue() != v.value.intValue());
    }

    public InstantValue ge(InstantValue v) {
        if (value instanceof Float || v.value instanceof Float)
            return new InstantValue(value.floatValue() >= v.value.floatValue());
        return new InstantValue(value.intValue() >= v.value.intValue());
    }

    public InstantValue gt(InstantValue v) {
        if (value instanceof Float || v.value instanceof Float)
            return new InstantValue(value.floatValue() > v.value.floatValue());
        return new InstantValue(value.intValue() > v.value.intValue());
    }

    public InstantValue le(InstantValue v) {
        if (value instanceof Float || v.value instanceof Float)
            return new InstantValue(value.floatValue() <= v.value.floatValue());
        return new InstantValue(value.intValue() <= v.value.intValue());
    }

    public InstantValue lt(InstantValue v) {
        if (value instanceof Float || v.value instanceof Float)
            return new InstantValue(value.floatValue() < v.value.floatValue());
        return new InstantValue(value.intValue() < v.value.intValue());
    }

    public InstantValue neg() {
        if (value instanceof Float)
            return new InstantValue(-value.floatValue());
        return new InstantValue(-value.intValue());
    }

    public InstantValue lNot() {
        if (value instanceof Float)
            return new InstantValue(value.floatValue() == 0.0f);
        return new InstantValue(value.intValue() == 0);
    }

    public InstantValue abs() {
        if (value instanceof Float)
            return new InstantValue(Math.abs(value.floatValue()));
        return new InstantValue(Math.abs(value.intValue()));
    }

    public InstantValue toInt() {
        return new InstantValue(value.intValue());
    }

    public InstantValue toFloat() {
        return new InstantValue(value.floatValue());
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
        InstantValue value1 = (InstantValue) o;
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
