package compile.vir.ir;

import compile.vir.type.BasicType;

public class LiVIR extends VIR {
    public final Number value;

    public LiVIR(Number value) {
        super(switch (value) {
            case Integer iVal -> BasicType.I32;
            case Float fVal -> BasicType.FLOAT;
            default -> throw new IllegalStateException("Unexpected value: " + value);
        });
        this.value = value;
    }

    @Override
    public VIR copy() {
        return new LiVIR(value);
    }

    @Override
    public String toString() {
        return String.format("LI      %%%d, #%s", id, value);
    }
}
