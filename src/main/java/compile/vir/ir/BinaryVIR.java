package compile.vir.ir;

import compile.vir.VReg;
import compile.vir.type.BasicType;
import compile.vir.value.Value;

import java.util.List;

public class BinaryVIR extends VIR {
    public final Type type;
    public final Value left, right;

    public BinaryVIR(Type type, Value left, Value right) {
        super(switch (type) {
            case ADD, SUB, MUL, DIV, MOD ->
                    left.getType() == BasicType.FLOAT || right.getType() == BasicType.FLOAT ? BasicType.FLOAT : BasicType.I32;
            case EQ, NE, GE, GT, LE, LT -> BasicType.I32;
        });
        this.type = type;
        this.left = left;
        this.right = right;
    }

    @Override
    public VIR copy() {
        return new BinaryVIR(type, left, right);
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
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(type);
        builder.append(" ".repeat(8 - builder.length()));
        builder.append("%").append(id).append(", ").append(left instanceof VIR ir ? ir.getTag() : left).append(", ").append(right instanceof VIR ir ? ir.getTag() : right);
        return builder.toString();
    }

    public enum Type {
        ADD, SUB, MUL, DIV, MOD, EQ, NE, GE, GT, LE, LT
    }
}
