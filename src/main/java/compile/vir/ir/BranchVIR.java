package compile.vir.ir;

import compile.vir.Block;
import compile.vir.VReg;
import compile.vir.type.BasicType;
import compile.vir.value.Value;

import java.util.List;

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
    public VIR copy() {
        return new BranchVIR(type, left, right, trueBlock, falseBlock);
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
        return "B" + type + "     " + (left instanceof VIR ir ? ir.getTag() : left) + ", " + (right instanceof VIR ir ? ir.getTag() : right) + ", " + trueBlock + ", " + falseBlock;
    }

    public enum Type {
        EQ, NE, GE, GT, LE, LT
    }
}
