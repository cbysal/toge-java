package compile.codegen.virgen.vir;

import compile.codegen.virgen.VReg;

import java.util.List;

public record RetVIR(VReg retVal) implements VIR {
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
