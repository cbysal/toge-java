package compile.vir.ir;

import compile.vir.VReg;
import compile.vir.value.Value;

import java.util.List;

public class MovVIR extends VIR {
    public final VReg target;
    public final Value source;

    public MovVIR(VReg target, Value source) {
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
        if (source instanceof VReg reg)
            return List.of(reg);
        return List.of();
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
