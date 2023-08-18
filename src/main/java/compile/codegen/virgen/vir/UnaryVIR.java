package compile.codegen.virgen.vir;

import compile.codegen.virgen.VReg;

import java.util.List;

public class UnaryVIR extends VIR {
    public enum Type {
        F2I, I2F, NEG, L_NOT, ABS
    }

    private final Type type;
    private final VReg target;
    private final VIRItem source;

    public UnaryVIR(Type type, VReg target, VIRItem source) {
        this.type = type;
        this.target = target;
        this.source = source;
    }

    public Type type() {
        return type;
    }

    public VReg target() {
        return target;
    }

    public VIRItem source() {
        return source;
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
}
