package compile.codegen.machine.asm;

import compile.codegen.machine.reg.MReg;
import compile.codegen.machine.reg.Reg;
import compile.codegen.machine.reg.VReg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record CvtAsm(Reg dest, Reg src) implements Asm {
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
            return new CvtAsm(vRegToMReg.get(vDest), vRegToMReg.get(vSrc));
        }
        if (dest instanceof VReg vDest && vRegToMReg.containsKey(vDest)) {
            return new CvtAsm(vRegToMReg.get(vDest), src);
        }
        if (src instanceof VReg vSrc && vRegToMReg.containsKey(vSrc)) {
            return new CvtAsm(dest, vRegToMReg.get(vSrc));
        }
        return this;
    }

    @Override
    public List<Asm> spill(Map<VReg, Integer> vRegToSpill) {
        if (dest instanceof VReg && vRegToSpill.containsKey(dest) && src instanceof VReg && vRegToSpill.containsKey(src)) {
            VReg newDest = new VReg(dest.isFloat());
            VReg newSrc = new VReg(src.isFloat());
            int spill1 = vRegToSpill.get(dest);
            int spill2 = vRegToSpill.get(src);
            return List.of(new VLoadSpillAsm(newSrc, spill2), new CvtAsm(newDest, newSrc), new VStoreSpillAsm(newDest, spill1));
        }
        if (dest instanceof VReg && vRegToSpill.containsKey(dest)) {
            VReg newDest = new VReg(dest.isFloat());
            int spill = vRegToSpill.get(dest);
            return List.of(new CvtAsm(newDest, src), new VStoreSpillAsm(newDest, spill));
        }
        if (src instanceof VReg && vRegToSpill.containsKey(src)) {
            VReg newSrc = new VReg(src.isFloat());
            int spill = vRegToSpill.get(src);
            return List.of(new VLoadSpillAsm(newSrc, spill), new CvtAsm(dest, newSrc));
        }
        return List.of(this);
    }

    @Override
    public String toString() {
        if (dest.isFloat() && !src.isFloat()) {
            return String.format("fcvt.s.w %s,%s", dest, src);
        }
        if (!dest.isFloat() && src.isFloat()) {
            return String.format("fcvt.w.s %s,%s,rtz", dest, src);
        }
        throw new RuntimeException();
    }
}
