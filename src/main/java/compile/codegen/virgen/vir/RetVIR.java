package compile.codegen.virgen.vir;

import compile.codegen.virgen.VReg;

import java.util.List;

public class RetVIR extends VIR {
    private final VReg retVal;

    public RetVIR(VReg retVal) {
        this.retVal = retVal;
    }

    public VReg retVal() {
        return retVal;
    }

    @Override
    public List<VReg> getRead() {
        if (retVal == null)
            return List.of();
        return List.of(retVal);
    }

    @Override
    public String toString() {
        return "RET     " + retVal;
    }
}
