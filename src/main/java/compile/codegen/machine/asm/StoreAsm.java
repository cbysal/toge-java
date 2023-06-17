package compile.codegen.machine.asm;

import compile.codegen.machine.reg.MReg;
import compile.codegen.machine.reg.Reg;
import compile.codegen.machine.reg.VReg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record StoreAsm(Reg src, Reg dest, int offset, int size) implements Asm {
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
        if (src instanceof VReg vSrc && vRegToMReg.containsKey(vSrc) && dest instanceof VReg vDest && vRegToMReg.containsKey(vDest)) {
            return new StoreAsm(vRegToMReg.get(vSrc), vRegToMReg.get(vDest), offset, size);
        }
        if (src instanceof VReg vSrc && vRegToMReg.containsKey(vSrc)) {
            return new StoreAsm(vRegToMReg.get(vSrc), dest, offset, size);
        }
        if (dest instanceof VReg vDest && vRegToMReg.containsKey(vDest)) {
            return new StoreAsm(src, vRegToMReg.get(vDest), offset, size);
        }
        return this;
    }

    @Override
    public List<Asm> spill(Map<VReg, Integer> vRegToSpill) {
        if (src instanceof VReg && vRegToSpill.containsKey(src) && dest instanceof VReg && vRegToSpill.containsKey(dest)) {
            VReg newSrc = new VReg(src.isFloat());
            VReg newDest = new VReg(dest.isFloat());
            int spill1 = vRegToSpill.get(src);
            int spill2 = vRegToSpill.get(dest);
            return List.of(new VLoadSpillAsm(newDest, spill1), new VLoadSpillAsm(newSrc, spill2), new StoreAsm(newSrc, newDest, offset, size));
        }
        if (src instanceof VReg && vRegToSpill.containsKey(src)) {
            VReg newSrc = new VReg(src.isFloat());
            int spill = vRegToSpill.get(src);
            return List.of(new VLoadSpillAsm(newSrc, spill), new StoreAsm(newSrc, dest, offset, size));
        }
        if (dest instanceof VReg && vRegToSpill.containsKey(dest)) {
            VReg newDest = new VReg(dest.isFloat());
            int spill = vRegToSpill.get(dest);
            return List.of(new VLoadSpillAsm(newDest, spill), new StoreAsm(src, newDest, offset, size));
        }
        return List.of(this);
    }

    @Override
    public String toString() {
        if (src.isFloat()) {
            return String.format("fsw %s,%d(%s)", src, offset, dest);
        }
        return switch (size) {
            case 4 -> String.format("sw %s,%d(%s)", src, offset, dest);
            case 8 -> String.format("sd %s,%d(%s)", src, offset, dest);
            default -> throw new RuntimeException();
        };
    }
}
