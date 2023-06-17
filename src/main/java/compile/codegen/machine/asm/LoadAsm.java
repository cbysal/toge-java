package compile.codegen.machine.asm;

import compile.codegen.machine.reg.MReg;
import compile.codegen.machine.reg.Reg;
import compile.codegen.machine.reg.VReg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record LoadAsm(Reg dest, Reg src, int offset, int size) implements Asm {
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
            return new LoadAsm(vRegToMReg.get(vDest), vRegToMReg.get(vSrc), offset, size);
        }
        if (dest instanceof VReg vDest && vRegToMReg.containsKey(vDest)) {
            return new LoadAsm(vRegToMReg.get(vDest), src, offset, size);
        }
        if (src instanceof VReg vSrc && vRegToMReg.containsKey(vSrc)) {
            return new LoadAsm(dest, vRegToMReg.get(vSrc), offset, size);
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
            return List.of(new VLoadSpillAsm(newSrc, spill2), new LoadAsm(newDest, newSrc, offset, size), new VStoreSpillAsm(newDest, spill1));
        }
        if (dest instanceof VReg && vRegToSpill.containsKey(dest)) {
            VReg newDest = new VReg(dest.isFloat());
            int spill = vRegToSpill.get(dest);
            return List.of(new LoadAsm(newDest, src, offset, size), new VStoreSpillAsm(newDest, spill));
        }
        if (src instanceof VReg && vRegToSpill.containsKey(src)) {
            VReg newSrc = new VReg(src.isFloat());
            int spill = vRegToSpill.get(src);
            return List.of(new VLoadSpillAsm(newSrc, spill), new LoadAsm(dest, newSrc, offset, size));
        }
        return List.of(this);
    }

    @Override
    public String toString() {
        if (dest.isFloat()) {
            return String.format("flw %s,%d(%s)", dest, offset, src);
        }
        return switch (size) {
            case 4 -> String.format("lw %s,%d(%s)", dest, offset, src);
            case 8 -> String.format("ld %s,%d(%s)", dest, offset, src);
            default -> throw new RuntimeException();
        };
    }
}
