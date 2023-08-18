package compile.codegen.virgen.vir;

import compile.codegen.virgen.VReg;

public class LiVIR extends VIR {
    private final VReg target;
    private final Number value;

    public LiVIR(VReg target, Number value) {
        this.target = target;
        this.value = value;
    }

    public VReg target() {
        return target;
    }

    public Number value() {
        return value;
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
