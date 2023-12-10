package compile.vir.ir;

import compile.vir.Block;
import compile.vir.type.BasicType;
import compile.vir.value.Value;

public class BranchVIR extends VIR {
    public final Type type;
    public final Value left, right;
    public final Block trueBlock, falseBlock;

    public BranchVIR(Type type, Value left, Value right, Block trueBlock, Block falseBlock) {
        super(BasicType.VOID);
        this.type = type;
        this.left = left;
        this.right = right;
        this.trueBlock = trueBlock;
        this.falseBlock = falseBlock;
    }

    @Override
    public String toString() {
        return "B" + type + "     " + (left instanceof VIR ir ? ir.getName() : left) + ", " + (right instanceof VIR ir ? ir.getName() : right) + ", " + trueBlock + ", " + falseBlock;
    }

    public enum Type {
        EQ, NE, GE, GT, LE, LT
    }
}
