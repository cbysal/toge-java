package compile.vir.ir;

import compile.vir.VReg;

public class LiVIR extends VIR {
    public final VReg target;
    public final Number value;

    public LiVIR(VReg target, Number value) {
        super(target.getType());
        this.target = target;
        this.value = value;
    }

    @Override
    public VIR copy() {
        return new LiVIR(target, value);
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
