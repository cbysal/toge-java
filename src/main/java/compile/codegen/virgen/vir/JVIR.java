package compile.codegen.virgen.vir;

import compile.codegen.virgen.Block;
import compile.codegen.virgen.VReg;

import java.util.List;

public class JVIR implements VIR {
    private final Block block;

    public JVIR(Block block) {
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }

    @Override
    public List<VReg> getRead() {
        return List.of();
    }

    @Override
    public VReg getWrite() {
        return null;
    }

    @Override
    public String toString() {
        return "J       " + block;
    }
}
