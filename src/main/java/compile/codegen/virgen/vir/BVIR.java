package compile.codegen.virgen.vir;

import compile.codegen.virgen.Block;
import compile.codegen.virgen.VReg;

import java.util.List;

public class BVIR implements VIR {
    public enum Type {
        EQ, GE, GT, LE, LT, NE
    }

    private final Type type;
    private final VIRItem left, right;
    private final Block trueBlock, falseBlock;

    public BVIR(Type type, VIRItem left, VIRItem right, Block trueBlock, Block falseBlock) {
        this.type = type;
        this.left = left;
        this.right = right;
        this.trueBlock = trueBlock;
        this.falseBlock = falseBlock;
    }

    public Type getType() {
        return type;
    }

    public VIRItem getLeft() {
        return left;
    }

    public VIRItem getRight() {
        return right;
    }

    public Block getTrueBlock() {
        return trueBlock;
    }

    public Block getFalseBlock() {
        return falseBlock;
    }

    @Override
    public List<VReg> getRead() {
        if (left == null || right == null)
            return List.of();
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
        return null;
    }

    @Override
    public String toString() {
        return "B" + type + "     " + left + ", " + right + ", " + trueBlock + ", " + falseBlock;
    }
}
