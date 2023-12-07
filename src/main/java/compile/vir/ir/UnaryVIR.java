package compile.vir.ir;

import compile.vir.VReg;
import compile.vir.value.Value;

import java.util.List;

public class UnaryVIR extends VIR {
    public final Type type;
    public final VReg target;
    public final Value source;

    public UnaryVIR(Type type, VReg target, Value source) {
        super(target.getType());
        this.type = type;
        this.target = target;
        this.source = source;
    }

    @Override
    public VIR copy() {
        return new UnaryVIR(type, target, source);
    }

    @Override
    public List<VReg> getRead() {
        if (source instanceof VReg reg)
            return List.of(reg);
        return List.of();
    }

    @Override
    public VReg getWrite() {
        return target;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(type);
        builder.append(" ".repeat(8 - builder.length()));
        builder.append(target).append(", ").append(source);
        return builder.toString();
    }

    public enum Type {
        F2I, I2F, NEG, L_NOT, ABS
    }
}
