package compile.codegen.virgen.vir;

import compile.codegen.virgen.Block;
import compile.codegen.virgen.VReg;

import java.util.List;

public class BVIR implements VIR {
    public enum Type {
        EQ, GE, GT, LE, LT, NE
    }

    private final Type type;
    private final Block block;
    private final VIRItem left, right;

    public BVIR(Type type, Block block, VIRItem left, VIRItem right) {
        this.type = type;
        this.block = block;
        this.left = left;
        this.right = right;
    }

    public Block getBlock() {
        return block;
    }

    public VIRItem getLeft() {
        return left;
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

    public VIRItem getRight() {
        return right;
    }

    public Type getType() {
        return type;
    }

    @Override
    public VReg getWrite() {
        return null;
    }

    @Override
    public String toString() {
        return "B" + type + "     " + block + ", " + left + ", " + right;
    }
}
