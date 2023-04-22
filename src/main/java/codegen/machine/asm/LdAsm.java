package codegen.machine.asm;

import codegen.machine.reg.MReg;
import codegen.machine.reg.Reg;
import codegen.machine.reg.VReg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LdAsm implements Asm {
    private Reg dest, src;
    private final int offset;

    public LdAsm(Reg dest, Reg src) {
        this(dest, src, 0);
    }

    public LdAsm(Reg dest, Reg src, int offset) {
        this.dest = dest;
        this.src = src;
        this.offset = offset;
    }

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
    public void replaceVRegs(Map<VReg, MReg> vRegToMReg) {
        if (dest instanceof VReg vReg && vRegToMReg.containsKey(vReg)) {
            dest = vRegToMReg.get(vReg);
        }
        if (src instanceof VReg vReg && vRegToMReg.containsKey(vReg)) {
            src = vRegToMReg.get(vReg);
        }
    }

    @Override
    public List<Asm> spill(Map<VReg, Integer> vRegToSpill) {
        if (dest instanceof VReg && vRegToSpill.containsKey(dest) && src instanceof VReg && vRegToSpill.containsKey(src)) {
            int spill1 = vRegToSpill.get(dest);
            int spill2 = vRegToSpill.get(src);
            return List.of(new LdAsm(MReg.T2, MReg.SP, spill2), new LdAsm(MReg.T2, MReg.T2, offset),
                    new SdAsm(MReg.T2, MReg.SP, spill1));
        }
        if (dest instanceof VReg && vRegToSpill.containsKey(dest)) {
            int spill1 = vRegToSpill.get(dest);
            return List.of(new LdAsm(MReg.T2, src, offset), new SdAsm(MReg.T2, MReg.SP, spill1));
        }
        if (src instanceof VReg && vRegToSpill.containsKey(src)) {
            int spill2 = vRegToSpill.get(src);
            return List.of(new LdAsm(MReg.T2, MReg.SP, spill2), new LdAsm(dest, MReg.T2, offset));
        }
        return List.of(this);
    }

    @Override
    public String toString() {
        return String.format("ld %s,%d(%s)", dest, offset, src);
    }
}
