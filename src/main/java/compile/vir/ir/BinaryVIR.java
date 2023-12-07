package compile.vir.ir;

import compile.vir.VReg;
import compile.vir.value.Value;

import java.util.List;

public class BinaryVIR extends VIR {
    public final Type type;
    public final VReg target;
    public final Value left, right;

    public BinaryVIR(Type type, VReg target, Value left, Value right) {
        super(target.getType());
        this.type = type;
        this.target = target;
        this.left = left;
        this.right = right;
    }

    @Override
    public VIR copy() {
        return new BinaryVIR(type, target, left, right);
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
