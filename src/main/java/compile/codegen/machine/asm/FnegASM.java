package compile.codegen.machine.asm;

import compile.codegen.machine.reg.MReg;
import compile.codegen.machine.reg.Reg;
import compile.codegen.machine.reg.VReg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record FnegASM(Reg dest, Reg src) implements Asm {
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
            return new FnegASM(vRegToMReg.get(vDest), vRegToMReg.get(vSrc));
        }
        if (dest instanceof VReg vDest && vRegToMReg.containsKey(vDest)) {
            return new FnegASM(vRegToMReg.get(vDest), src);
        }
        if (src instanceof VReg vSrc && vRegToMReg.containsKey(vSrc)) {
            return new FnegASM(dest, vRegToMReg.get(vSrc));
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
            return List.of(new VLoadSpillAsm(newSrc, spill2), new FnegASM(newDest, newSrc), new VStoreSpillAsm(newDest, spill1));
        }
        if (dest instanceof VReg && vRegToSpill.containsKey(dest)) {
            VReg newDest = new VReg(dest.isFloat());
            int spill = vRegToSpill.get(dest);
            return List.of(new FnegASM(newDest, src), new VStoreSpillAsm(newDest, spill));
        }
        if (src instanceof VReg && vRegToSpill.containsKey(src)) {
            VReg newSrc = new VReg(src.isFloat());
            int spill = vRegToSpill.get(src);
            return List.of(new VLoadSpillAsm(newSrc, spill), new FnegASM(dest, newSrc));
        }
        return List.of(this);
    }

    @Override
    public String toString() {
        return String.format("fneg.s %s,%s", dest, src);
    }
}
