package compile.vir.ir;

import compile.vir.type.BasicType;
import compile.vir.value.Value;

public class BinaryVIR extends VIR {
    public final Type type;
    public final Value left, right;

    public BinaryVIR(Type type, Value left, Value right) {
        super(switch (type) {
            case XOR -> BasicType.I1;
            case ADD, FADD, SUB, FSUB, MUL, FMUL, SDIV, FDIV, SREM ->
                    left.getType() == BasicType.FLOAT || right.getType() == BasicType.FLOAT ? BasicType.FLOAT : BasicType.I32;
            case EQ, NE, GE, GT, LE, LT -> BasicType.I32;
        });
        this.type = type;
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        return String.format("%s = %s %s %s, %s", getName(), type.toString().toLowerCase(), left.getType(), left.getName(), right.getName());
    }

    public enum Type {
        ADD, FADD, SUB, FSUB, MUL, FMUL, SDIV, FDIV, SREM, XOR, EQ, NE, GE, GT, LE, LT
    }
}
