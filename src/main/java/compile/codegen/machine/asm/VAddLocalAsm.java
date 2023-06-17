package compile.codegen.machine.asm;

import compile.codegen.machine.reg.MReg;
import compile.codegen.machine.reg.Reg;
import compile.codegen.machine.reg.VReg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record VAddLocalAsm(Reg dest, Reg src, int local) implements Asm {
    @Override
    public List<VReg> getVRegs() {
        List<VReg> vRegs = new ArrayList<>();
        if (dest instanceof VReg vReg) {
            vRegs.add(vReg);
        }
        if (src instanceof VReg vReg) {
            vRegs.add(vReg);
        }
        return vRegs;
    }

    @Override
    public Asm replaceVRegs(Map<VReg, MReg> vRegToMReg) {
        if (dest instanceof VReg vDest && vRegToMReg.containsKey(vDest) && src instanceof VReg vSrc && vRegToMReg.containsKey(vSrc)) {
            return new VAddLocalAsm(vRegToMReg.get(vDest), vRegToMReg.get(vSrc), local);
        }
        if (dest instanceof VReg vDest && vRegToMReg.containsKey(vDest)) {
            return new VAddLocalAsm(vRegToMReg.get(vDest), src, local);
        }
        if (src instanceof VReg vSrc && vRegToMReg.containsKey(vSrc)) {
            return new VAddLocalAsm(dest, vRegToMReg.get(vSrc), local);
        }
        return this;
    }

    @Override
    public List<Asm> spill(Map<VReg, Integer> vRegToSpill) {
        if (dest instanceof VReg && vRegToSpill.containsKey(dest) && src instanceof VReg && vRegToSpill.containsKey(src)) {
            VReg newDest = new VReg(false);
            VReg newSrc = new VReg(false);
            int spill1 = vRegToSpill.get(dest);
            int spill2 = vRegToSpill.get(src);
            return List.of(new VLoadSpillAsm(newSrc, spill2), new VAddLocalAsm(newDest, newSrc, local), new VStoreSpillAsm(newDest, spill1));
        }
        if (dest instanceof VReg && vRegToSpill.containsKey(dest)) {
            VReg newDest = new VReg(false);
            int spill = vRegToSpill.get(dest);
            return List.of(new VAddLocalAsm(newDest, src, local), new VStoreSpillAsm(newDest, spill));
        }
        if (src instanceof VReg && vRegToSpill.containsKey(src)) {
            VReg newSrc = new VReg(false);
            int spill = vRegToSpill.get(src);
            return List.of(new VLoadSpillAsm(newSrc, spill), new VAddLocalAsm(dest, newSrc, local));
        }
        return List.of(this);
    }
}
