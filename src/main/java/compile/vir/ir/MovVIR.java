package compile.vir.ir;

import compile.vir.VReg;

import java.util.List;

public class MovVIR extends VIR {
    public final VReg target;
    public final VReg source;

    public MovVIR(VReg target, VReg source) {
        super(target.getType());
        this.target = target;
        this.source = source;
    }

    @Override
    public VIR copy() {
        return new MovVIR(target, source);
    }

    @Override
    public List<VReg> getRead() {
        return List.of(source);
    }

    @Override
    public VReg getWrite() {
        return target;
    }

    @Override
    public String toString() {
        return "MOV     " + target + ", " + source;
    }
}
