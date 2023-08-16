package compile.codegen.virgen.vir;

import compile.codegen.virgen.Block;

public record JumpVIR(Block target) implements VIR {
    @Override
    public String toString() {
        return "B       " + target;
    }
}
