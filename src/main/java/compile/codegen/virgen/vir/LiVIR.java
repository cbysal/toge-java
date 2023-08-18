package compile.codegen.virgen.vir;

import compile.codegen.virgen.VReg;

public class LiVIR extends VIR {
    public final VReg target;
    public final Number value;

    public LiVIR(VReg target, Number value) {
        this.target = target;
        this.value = value;
    }

    @Override
    public VReg getWrite() {
        return target;
    }

    @Override
    public String toString() {
        return "LI      " + target + ", #" + value;
    }
}
