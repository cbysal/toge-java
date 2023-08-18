package compile.codegen.virgen.vir;

import compile.codegen.virgen.Block;

public class JumpVIR extends VIR {
    private final Block target;

    public JumpVIR(Block target) {
        this.target = target;
    }

    public Block target() {
        return target;
    }

    @Override
    public String toString() {
        return "B       " + target;
    }
}
