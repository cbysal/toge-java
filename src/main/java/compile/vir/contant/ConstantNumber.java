package compile.vir.contant;

import compile.vir.type.BasicType;

import java.util.Objects;

public final class ConstantNumber extends Constant {
    private final Number value;

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
        return toString();
    }

    @Override
    public String toString() {
        return "#" + value;
    }
}
