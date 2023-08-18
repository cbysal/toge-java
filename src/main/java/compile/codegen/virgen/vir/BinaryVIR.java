package compile.codegen.virgen.vir;

import compile.codegen.virgen.VReg;

import java.util.List;

public class BinaryVIR extends VIR {
    public final Type type;
    public final VReg target;
    public final VIRItem left, right;
    public BinaryVIR(Type type, VReg target, VIRItem left, VIRItem right) {
        this.type = type;
        this.target = target;
        this.left = left;
        this.right = right;
    }

    @Override
    public List<VReg> getRead() {
        if (left instanceof VReg reg1 && right instanceof VReg reg2)
            return List.of(reg1, reg2);
        if (left instanceof VReg reg1)
            return List.of(reg1);
        if (right instanceof VReg reg2)
            return List.of(reg2);
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
        builder.append(target).append(", ").append(left).append(", ").append(right);
        return builder.toString();
    }

    public enum Type {
        ADD, SUB, MUL, DIV, MOD, EQ, NE, GE, GT, LE, LT
    }
}
