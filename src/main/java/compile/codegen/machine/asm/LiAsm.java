package compile.codegen.machine.asm;

import compile.codegen.machine.reg.MReg;
import compile.codegen.machine.reg.Reg;
import compile.codegen.machine.reg.VReg;

import java.util.List;
import java.util.Map;

public class LiAsm implements Asm {
    private Reg dest;
    private final int imm;

    public LiAsm(Reg dest, int imm) {
        this.dest = dest;
        this.imm = imm;
    }

    @Override
    public List<VReg> getVRegs() {
        if (dest instanceof VReg vReg) {
            return List.of(vReg);
        }
        return List.of();
    }

    @Override
    public void replaceVRegs(Map<VReg, MReg> vRegToMReg) {
        if (dest instanceof VReg vReg && vRegToMReg.containsKey(vReg)) {
            dest = vRegToMReg.get(vReg);
        }
    }

    @Override
    public List<Asm> spill(Map<VReg, Integer> vRegToSpill) {
        if (dest instanceof VReg && vRegToSpill.containsKey(dest)) {
            int spill = vRegToSpill.get(dest);
            return List.of(new LiAsm(MReg.T2, imm), new SdAsm(MReg.T2, MReg.SP, spill));
        }
        return List.of(this);
    }

    @Override
    public String toString() {
        return String.format("li %s,%d", dest, imm);
    }
}
