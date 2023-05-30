package compile.codegen.machine.asm;

import compile.codegen.machine.reg.MReg;
import compile.codegen.machine.reg.Reg;
import compile.codegen.machine.reg.VReg;

import java.util.List;
import java.util.Map;

public record LiAsm(Reg dest, int imm) implements Asm {
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
            return new LiAsm(vRegToMReg.get(vDest), imm);
        }
        return this;
    }

    @Override
    public List<Asm> spill(Map<VReg, Integer> vRegToSpill) {
        if (dest instanceof VReg && vRegToSpill.containsKey(dest)) {
            int spill = vRegToSpill.get(dest);
            return List.of(new LiAsm(MReg.T2, imm), new StoreAsm(MReg.T2, MReg.SP, spill, 8));
        }
        return List.of(this);
    }

    @Override
    public String toString() {
        return String.format("li %s,%d", dest, imm);
    }
}
