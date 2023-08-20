package compile.codegen.virgen.vir;

import compile.codegen.virgen.VReg;

import java.util.List;

public class RetVIR extends VIR {
    public final VIRItem retVal;

    public RetVIR(VIRItem retVal) {
        this.retVal = retVal;
    }

    @Override
    public VIR copy() {
        return new RetVIR(retVal);
    }

    @Override
    public List<VReg> getRead() {
        if (retVal instanceof VReg reg)
            return List.of(reg);
        return List.of();
    }

    @Override
    public String toString() {
        if (retVal == null)
            return "RET";
        return "RET     " + retVal;
    }
}
