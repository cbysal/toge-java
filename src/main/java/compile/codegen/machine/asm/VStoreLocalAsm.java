package compile.codegen.machine.asm;

import compile.codegen.machine.reg.MReg;
import compile.codegen.machine.reg.Reg;
import compile.codegen.machine.reg.VReg;

import java.util.List;
import java.util.Map;

public record VStoreLocalAsm(Reg src, int dest) implements Asm {
    @Override
    public List<VReg> getVRegs() {
        if (src instanceof VReg vReg) {
            return List.of(vReg);
        }
        return List.of();
    }

    @Override
    public Asm replaceVRegs(Map<VReg, MReg> vRegToMReg) {
        if (src instanceof VReg vSrc && vRegToMReg.containsKey(vSrc)) {
            return new VStoreLocalAsm(vRegToMReg.get(vSrc), dest);
        }
        return this;
    }

    @Override
    public List<Asm> spill(Map<VReg, Integer> vRegToSpill) {
        if (src instanceof VReg && vRegToSpill.containsKey(src)) {
            VReg newSrc = new VReg(src.isFloat());
            int spill = vRegToSpill.get(src);
            return List.of(new VLoadSpillAsm(newSrc, spill), new VStoreLocalAsm(newSrc, dest));
        }
        return List.of(this);
    }
}
