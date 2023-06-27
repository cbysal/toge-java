package compile.codegen.virgen.vir;

import compile.codegen.virgen.VReg;

import java.util.List;

public class UnaryVIR implements VIR {
    public enum Type {
        F2I, I2F, L_NOT, NEG
    }

    private final Type type;
    private final VReg result;
    private final VIRItem source;

    public UnaryVIR(Type type, VReg result, VIRItem source) {
        this.type = type;
        this.result = result;
        this.source = source;
    }

    @Override
    public List<VReg> getRead() {
        if (source instanceof VReg reg)
            return List.of(reg);
        return List.of();
    }

    public VReg getResult() {
        return result;
    }

    public VIRItem getSource() {
        return source;
    }

    public Type getType() {
        return type;
    }

    @Override
    public VReg getWrite() {
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(type);
        builder.append(" ".repeat(8 - builder.length()));
        builder.append(result).append(", ").append(source);
        return builder.toString();
    }
}
