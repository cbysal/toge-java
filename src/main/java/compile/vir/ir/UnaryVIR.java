package compile.vir.ir;

import compile.vir.VReg;
import compile.vir.type.BasicType;
import compile.vir.value.Value;

import java.util.List;

public class UnaryVIR extends VIR {
    public final Type type;
    public final Value source;

    public UnaryVIR(Type type, Value source) {
        super(switch (type) {
            case F2I, L_NOT -> BasicType.I32;
            case I2F -> BasicType.FLOAT;
            case NEG, ABS -> source.getType();
        });
        this.type = type;
        this.source = source;
    }

    @Override
    public VIR copy() {
        return new UnaryVIR(type, source);
    }

    @Override
    public List<VReg> getRead() {
        if (source instanceof VReg reg)
            return List.of(reg);
        return List.of();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(type);
        builder.append(" ".repeat(8 - builder.length()));
        builder.append(getTag()).append(", ").append(source instanceof VIR ir ? ir.getTag() : source);
        return builder.toString();
    }

    public enum Type {
        F2I, I2F, NEG, L_NOT, ABS
    }
}
