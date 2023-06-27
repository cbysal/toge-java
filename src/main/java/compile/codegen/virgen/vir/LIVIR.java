package compile.codegen.virgen.vir;

import compile.codegen.virgen.VReg;
import compile.symbol.Type;

import java.util.List;

public class LIVIR implements VIR {
    private final VReg target;
    private final int value;

    public LIVIR(VReg target, float value) {
        this.target = target;
        this.value = Float.floatToIntBits(value);
    }

    public LIVIR(VReg target, int value) {
        this.target = target;
        this.value = value;
    }

    @Override
    public List<VReg> getRead() {
        return List.of();
    }

    public VReg getTarget() {
        return target;
    }

    public int getValue() {
        return value;
    }

    @Override
    public VReg getWrite() {
        return target;
    }

    @Override
    public String toString() {
        if (target.getType() == Type.FLOAT)
            return "LI      " + target + ", #" + Float.intBitsToFloat(value);
        else
            return "LI      " + target + ", #" + value;
    }
}
