package compile.codegen.virgen.vir;

import compile.codegen.virgen.VReg;

public record LIVIR(VReg target, Number value) implements VIR {
    @Override
    public VReg getWrite() {
        return target;
    }

    @Override
    public String toString() {
        return "LI      " + target + ", #" + value;
    }
}
