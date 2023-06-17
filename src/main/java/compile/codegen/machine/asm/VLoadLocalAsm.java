package compile.codegen.machine.asm;

import compile.codegen.machine.reg.MReg;
import compile.codegen.machine.reg.Reg;
import compile.codegen.machine.reg.VReg;

import java.util.List;
import java.util.Map;

public record VLoadLocalAsm(Reg dest, int src) implements Asm {
    @Override
    public List<VReg> getVRegs() {
        if (dest instanceof VReg vReg) {
            return List.of(vReg);
        }
        return List.of();
    }

    @Override
    public Asm replaceVRegs(Map<VReg, MReg> vRegToMReg) {
        if (dest instanceof VReg vDest && vRegToMReg.containsKey(vDest)) {
            return new VLoadLocalAsm(vRegToMReg.get(vDest), src);
        }
        return this;
    }

    @Override
    public List<Asm> spill(Map<VReg, Integer> vRegToSpill) {
        if (dest instanceof VReg && vRegToSpill.containsKey(dest)) {
            VReg newDest = new VReg(false);
            int spill = vRegToSpill.get(dest);
            return List.of(new VLoadLocalAsm(newDest, src), new VStoreSpillAsm(newDest, spill));
        }
        return List.of(this);
    }
}
