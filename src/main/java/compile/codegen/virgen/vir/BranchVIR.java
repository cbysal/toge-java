package compile.codegen.virgen.vir;

import compile.codegen.virgen.Block;
import compile.codegen.virgen.VReg;

import java.util.List;

public class BranchVIR extends VIR {
    public final Type type;
    public final VIRItem left, right;
    public final Block trueBlock, falseBlock;
    public BranchVIR(Type type, VIRItem left, VIRItem right, Block trueBlock, Block falseBlock) {
        this.type = type;
        this.left = left;
        this.right = right;
        this.trueBlock = trueBlock;
        this.falseBlock = falseBlock;
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
        return "B" + type + "     " + left + ", " + right + ", " + trueBlock + ", " + falseBlock;
    }

    public enum Type {
        EQ, NE, GE, GT, LE, LT
    }
}
