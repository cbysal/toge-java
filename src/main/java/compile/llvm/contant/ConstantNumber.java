package compile.llvm.contant;

import compile.llvm.type.BasicType;

import java.util.Objects;

public final class ConstantNumber extends Constant {
    private final Number value;

    public ConstantNumber(boolean value) {
        super(BasicType.I1);
        this.value = value ? 1 : 0;
    }

    public ConstantNumber(Number value) {
        super(switch (value) {
            case Integer iVal -> BasicType.I32;
            case Float fVal -> BasicType.FLOAT;
            default -> throw new IllegalStateException("Unexpected value: " + value);
        });
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
        return new ConstantNumber(switch (type) {
            case BasicType.I1, BasicType.I32 -> value.intValue() * number.value.intValue();
            case BasicType.FLOAT -> value.floatValue() * number.value.floatValue();
            default -> throw new IllegalStateException("Unexpected value: " + type);
        });
    }

    public ConstantNumber div(ConstantNumber number) {
        return new ConstantNumber(switch (type) {
            case BasicType.I1, BasicType.I32 -> value.intValue() / number.value.intValue();
            case BasicType.FLOAT -> value.floatValue() / number.value.floatValue();
            default -> throw new IllegalStateException("Unexpected value: " + type);
        });
    }

    public ConstantNumber rem(ConstantNumber number) {
        return new ConstantNumber(value.intValue() % number.value.intValue());
    }

    public ConstantNumber add(ConstantNumber number) {
        return new ConstantNumber(switch (type) {
            case BasicType.I1, BasicType.I32 -> value.intValue() + number.value.intValue();
            case BasicType.FLOAT -> value.floatValue() + number.value.floatValue();
            default -> throw new IllegalStateException("Unexpected value: " + type);
        });
    }

    public ConstantNumber sub(ConstantNumber number) {
        return new ConstantNumber(switch (type) {
            case BasicType.I1, BasicType.I32 -> value.intValue() - number.value.intValue();
            case BasicType.FLOAT -> value.floatValue() - number.value.floatValue();
            default -> throw new IllegalStateException("Unexpected value: " + type);
        });
    }

    public ConstantNumber xor(ConstantNumber number) {
        return switch (type) {
            case BasicType.I1 -> new ConstantNumber((value.intValue() ^ number.value.intValue()) != 0);
            case BasicType.I32 -> new ConstantNumber(value.intValue() ^ number.value.intValue());
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }

    public ConstantNumber neg() {
        return new ConstantNumber(switch (type) {
            case BasicType.I1, BasicType.I32 -> -value.intValue();
            case BasicType.FLOAT -> -value.floatValue();
            default -> throw new IllegalStateException("Unexpected value: " + type);
        });
    }

    public ConstantNumber lNot() {
        return new ConstantNumber(switch (type) {
            case BasicType.I1, BasicType.I32 -> value.intValue() == 0 ? 1 : 0;
            case BasicType.FLOAT -> value.floatValue() == 0.0f ? 1 : 0;
            default -> throw new IllegalStateException("Unexpected value: " + type);
        });
    }

    public ConstantNumber eq(ConstantNumber number) {
        return new ConstantNumber(switch (type) {
            case BasicType.I1, BasicType.I32 -> value.intValue() == number.value.intValue();
            case BasicType.FLOAT -> value.floatValue() == number.value.floatValue();
            default -> throw new IllegalStateException("Unexpected value: " + type);
        });
    }

    public ConstantNumber ne(ConstantNumber number) {
        return new ConstantNumber(switch (type) {
            case BasicType.I1, BasicType.I32 -> value.intValue() != number.value.intValue();
            case BasicType.FLOAT -> value.floatValue() != number.value.floatValue();
            default -> throw new IllegalStateException("Unexpected value: " + type);
        });
    }

    public ConstantNumber ge(ConstantNumber number) {
        return new ConstantNumber(switch (type) {
            case BasicType.I1, BasicType.I32 -> value.intValue() >= number.value.intValue();
            case BasicType.FLOAT -> value.floatValue() >= number.value.floatValue();
            default -> throw new IllegalStateException("Unexpected value: " + type);
        });
    }

    public ConstantNumber gt(ConstantNumber number) {
        return new ConstantNumber(switch (type) {
            case BasicType.I1, BasicType.I32 -> value.intValue() > number.value.intValue();
            case BasicType.FLOAT -> value.floatValue() > number.value.floatValue();
            default -> throw new IllegalStateException("Unexpected value: " + type);
        });
    }

    public ConstantNumber le(ConstantNumber number) {
        return new ConstantNumber(switch (type) {
            case BasicType.I1, BasicType.I32 -> value.intValue() <= number.value.intValue();
            case BasicType.FLOAT -> value.floatValue() <= number.value.floatValue();
            default -> throw new IllegalStateException("Unexpected value: " + type);
        });
    }

    public ConstantNumber lt(ConstantNumber number) {
        return new ConstantNumber(switch (type) {
            case BasicType.I1, BasicType.I32 -> value.intValue() < number.value.intValue();
            case BasicType.FLOAT -> value.floatValue() < number.value.floatValue();
            default -> throw new IllegalStateException("Unexpected value: " + type);
        });
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
        return switch (type) {
            case BasicType.I1 -> value.intValue() == 1 ? "true" : "false";
            case BasicType.I32 -> value.toString();
            case BasicType.FLOAT -> String.format("0x%X", Double.doubleToLongBits(value.floatValue()));
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }

    @Override
    public String toString() {
        return String.format("%s %s", type, getName());
    }
}
