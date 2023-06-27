package compile.codegen.virgen.vir;

import compile.codegen.virgen.VReg;

import java.util.List;

public class BinaryVIR implements VIR {
    public enum Type {
        ADD, DIV, EQ, GE, GT, LE, LT, MOD, MUL, NE, SUB
    }

    private final Type type;
    private final VReg result;
    private final VIRItem left, right;

    public BinaryVIR(Type type, VReg result, VIRItem left, VIRItem right) {
        this.type = type;
        this.result = result;
        this.left = left;
        this.right = right;
    }

    public VIRItem getLeft() {
        return left;
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

    public VReg getResult() {
        return result;
    }

    public VIRItem getRight() {
        return right;
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
        builder.append(result).append(", ").append(left).append(", ").append(right);
        return builder.toString();
    }
}
