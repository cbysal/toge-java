package compile.llvm.contant;

import compile.llvm.type.BasicType;

import java.util.Objects;

public final class ConstantNumber extends Constant {
    private final Number value;

    public ConstantNumber(boolean value) {
        super(BasicType.I1);
        this.value = value ? 1 : 0;
    }

    private static BasicType superHelper(Number value) {
        if (value instanceof Integer) {
            return BasicType.I32;
        }
        if (value instanceof Float) {
            return BasicType.FLOAT;
        }
        throw new RuntimeException();
    }

    public ConstantNumber(Number value) {
        super(superHelper(value));
        this.value = value;
    }

    public int intValue() {
        return value.intValue();
    }

    public float floatValue() {
        return value.floatValue();
    }

    public Number getValue() {
        return value;
    }

    public ConstantNumber mul(ConstantNumber number) {
        if (type.equals(BasicType.I1) || type.equals(BasicType.I32)) {
            return new ConstantNumber(value.intValue() * number.intValue());
        }
        if (type.equals(BasicType.FLOAT)) {
            return new ConstantNumber(value.floatValue() * number.floatValue());
        }
        throw new IllegalStateException("Unexpected value: " + type);
    }

    public ConstantNumber div(ConstantNumber number) {
        if (type.equals(BasicType.I1) || type.equals(BasicType.I32)) {
            return new ConstantNumber(value.intValue() / number.intValue());
        }
        if (type.equals(BasicType.FLOAT)) {
            return new ConstantNumber(value.floatValue() / number.floatValue());
        }
        throw new IllegalStateException("Unexpected value: " + type);
    }

    public ConstantNumber rem(ConstantNumber number) {
        return new ConstantNumber(value.intValue() % number.value.intValue());
    }

    public ConstantNumber add(ConstantNumber number) {
        if (type.equals(BasicType.I1) || type.equals(BasicType.I32)) {
            return new ConstantNumber(value.intValue() + number.intValue());
        }
        if (type.equals(BasicType.FLOAT)) {
            return new ConstantNumber(value.floatValue() + number.floatValue());
        }
        throw new IllegalStateException("Unexpected value: " + type);
    }

    public ConstantNumber sub(ConstantNumber number) {
        if (type.equals(BasicType.I1) || type.equals(BasicType.I32)) {
            return new ConstantNumber(value.intValue() - number.intValue());
        }
        if (type.equals(BasicType.FLOAT)) {
            return new ConstantNumber(value.floatValue() - number.floatValue());
        }
        throw new IllegalStateException("Unexpected value: " + type);
    }

    public ConstantNumber xor(ConstantNumber number) {
        if (type.equals(BasicType.I1)) {
            return new ConstantNumber((value.intValue() ^ number.value.intValue()) != 0);
        }
        if (type.equals(BasicType.I32)) {
            return new ConstantNumber(value.intValue() ^ number.value.intValue());
        }
        throw new IllegalStateException("Unexpected value: " + type);
    }

    public ConstantNumber neg() {
        if (type.equals(BasicType.I1) || type.equals(BasicType.I32)) {
            return new ConstantNumber(-value.intValue());
        }
        if (type.equals(BasicType.FLOAT)) {
            return new ConstantNumber(-value.floatValue());
        }
        throw new IllegalStateException("Unexpected value: " + type);
    }

    public ConstantNumber lNot() {
        if (type.equals(BasicType.I1) || type.equals(BasicType.I32)) {
            return new ConstantNumber(value.intValue() == 0 ? 1 : 0);
        }
        if (type.equals(BasicType.FLOAT)) {
            return new ConstantNumber(value.floatValue() == 0.0f ? 1 : 0);
        }
        throw new IllegalStateException("Unexpected value: " + type);
    }

    public ConstantNumber eq(ConstantNumber number) {
        if (type.equals(BasicType.I1) || type.equals(BasicType.I32)) {
            return new ConstantNumber(value.intValue() == number.intValue());
        }
        if (type.equals(BasicType.FLOAT)) {
            return new ConstantNumber(value.floatValue() == number.floatValue());
        }
        throw new IllegalStateException("Unexpected value: " + type);
    }

    public ConstantNumber ne(ConstantNumber number) {
        if (type.equals(BasicType.I1) || type.equals(BasicType.I32)) {
            return new ConstantNumber(value.intValue() != number.intValue());
        }
        if (type.equals(BasicType.FLOAT)) {
            return new ConstantNumber(value.floatValue() != number.floatValue());
        }
        throw new IllegalStateException("Unexpected value: " + type);
    }

    public ConstantNumber ge(ConstantNumber number) {
        if (type.equals(BasicType.I1) || type.equals(BasicType.I32)) {
            return new ConstantNumber(value.intValue() >= number.intValue());
        }
        if (type.equals(BasicType.FLOAT)) {
            return new ConstantNumber(value.floatValue() >= number.floatValue());
        }
        throw new IllegalStateException("Unexpected value: " + type);
    }

    public ConstantNumber gt(ConstantNumber number) {
        if (type.equals(BasicType.I1) || type.equals(BasicType.I32)) {
            return new ConstantNumber(value.intValue() > number.intValue());
        }
        if (type.equals(BasicType.FLOAT)) {
            return new ConstantNumber(value.floatValue() > number.floatValue());
        }
        throw new IllegalStateException("Unexpected value: " + type);
    }

    public ConstantNumber le(ConstantNumber number) {
        if (type.equals(BasicType.I1) || type.equals(BasicType.I32)) {
            return new ConstantNumber(value.intValue() <= number.intValue());
        }
        if (type.equals(BasicType.FLOAT)) {
            return new ConstantNumber(value.floatValue() <= number.floatValue());
        }
        throw new IllegalStateException("Unexpected value: " + type);
    }

    public ConstantNumber lt(ConstantNumber number) {
        if (type.equals(BasicType.I1) || type.equals(BasicType.I32)) {
            return new ConstantNumber(value.intValue() < number.intValue());
        }
        if (type.equals(BasicType.FLOAT)) {
            return new ConstantNumber(value.floatValue() < number.floatValue());
        }
        throw new IllegalStateException("Unexpected value: " + type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ConstantNumber value1 = (ConstantNumber) o;
        return Objects.equals(value, value1.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String getName() {
        if (type.equals(BasicType.I1)) {
            return value.intValue() == 1 ? "true" : "false";
        }
        if (type.equals(BasicType.I32)) {
            return value.toString();
        }
        if (type.equals(BasicType.FLOAT)) {
            return String.format("0x%X", Double.doubleToLongBits(value.floatValue()));
        }
        throw new IllegalStateException("Unexpected value: " + type);
    }

    @Override
    public String toString() {
        return String.format("%s %s", type, getName());
    }
}
