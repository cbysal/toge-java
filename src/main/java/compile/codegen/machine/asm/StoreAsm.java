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
            int spill1 = vRegToSpill.get(src);
            int spill2 = vRegToSpill.get(dest);
            return List.of(new LoadAsm(MReg.T2, MReg.SP, spill1, 8), new LoadAsm(MReg.T3, MReg.SP, spill2, 8), new StoreAsm(MReg.T2, MReg.T3, offset, size));
        }
        if (src instanceof VReg && vRegToSpill.containsKey(src)) {
            int spill1 = vRegToSpill.get(src);
            return List.of(new LoadAsm(MReg.T2, MReg.SP, spill1, 8), new StoreAsm(MReg.T2, dest, offset, size));
        }
        if (dest instanceof VReg && vRegToSpill.containsKey(dest)) {
            int spill2 = vRegToSpill.get(dest);
            return List.of(new LoadAsm(MReg.T3, MReg.SP, spill2, 8), new StoreAsm(src, MReg.T3, offset, size));
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
