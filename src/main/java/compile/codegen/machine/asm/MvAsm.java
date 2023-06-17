package compile.codegen.machine.asm;

import compile.codegen.machine.reg.MReg;
import compile.codegen.machine.reg.Reg;
import compile.codegen.machine.reg.VReg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record MvAsm(Reg dest, Reg src) implements Asm {
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
            return new MvAsm(vRegToMReg.get(vDest), vRegToMReg.get(vSrc));
        }
        if (dest instanceof VReg vDest && vRegToMReg.containsKey(vDest)) {
            return new MvAsm(vRegToMReg.get(vDest), src);
        }
        if (src instanceof VReg vSrc && vRegToMReg.containsKey(vSrc)) {
            return new MvAsm(dest, vRegToMReg.get(vSrc));
        }
        return this;
    }

    @Override
    public List<Asm> spill(Map<VReg, Integer> vRegToSpill) {
        if (dest instanceof VReg && vRegToSpill.containsKey(dest) && src instanceof VReg && vRegToSpill.containsKey(src)) {
            VReg newReg = new VReg(false);
            int spill1 = vRegToSpill.get(dest);
            int spill2 = vRegToSpill.get(src);
            return List.of(new VLoadSpillAsm(newReg, spill2), new VStoreSpillAsm(newReg, spill1));
        }
        if (dest instanceof VReg && vRegToSpill.containsKey(dest)) {
            int spill = vRegToSpill.get(dest);
            return List.of(new VStoreSpillAsm(src, spill));
        }
        if (src instanceof VReg && vRegToSpill.containsKey(src)) {
            int spill = vRegToSpill.get(src);
            return List.of(new VLoadSpillAsm(dest, spill));
        }
        return List.of(this);
    }

    @Override
    public String toString() {
        if (dest.isFloat() && src.isFloat()) {
            return String.format("fmv.s %s,%s", dest, src);
        }
        if (dest.isFloat()) {
            return String.format("fmv.w.x %s,%s", dest, src);
        }
        if (src.isFloat()) {
            return String.format("fmv.x.w %s,%s", dest, src);
        }
        return String.format("mv %s,%s", dest, src);
    }
}
