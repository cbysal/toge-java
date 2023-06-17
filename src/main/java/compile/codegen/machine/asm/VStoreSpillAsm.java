package compile.codegen.machine.asm;

import compile.codegen.machine.reg.MReg;
import compile.codegen.machine.reg.Reg;
import compile.codegen.machine.reg.VReg;

import java.util.List;
import java.util.Map;

public record VStoreSpillAsm(Reg src, int dest) implements Asm {
    @Override
    public List<VReg> getVRegs() {
        if (src instanceof VReg vSrc) {
            return List.of(vSrc);
        }
        return List.of();
    }

    @Override
    public Asm replaceVRegs(Map<VReg, MReg> vRegToMReg) {
        if (src instanceof VReg && vRegToMReg.containsKey(src)) {
            return new VStoreSpillAsm(vRegToMReg.get(src), dest);
        }
        return this;
    }

    @Override
    public List<Asm> spill(Map<VReg, Integer> vRegToSpill) {
        if (src instanceof VReg && vRegToSpill.containsKey(src)) {
            VReg newSrc = new VReg(src.isFloat());
            int spill = vRegToSpill.get(src);
            return List.of(new VLoadSpillAsm(newSrc, spill), new VStoreSpillAsm(newSrc, dest));
        }
        return List.of(this);
    }
}
