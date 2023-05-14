package compile.codegen.machine.asm;

import compile.codegen.machine.reg.MReg;
import compile.codegen.machine.reg.Reg;
import compile.codegen.machine.reg.VReg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SwAsm implements Asm {
    private Reg src, dest;
    private int offset;

    public SwAsm(Reg src, Reg dest) {
        this(src, dest, 0);
    }

    public SwAsm(Reg src, Reg dest, int offset) {
        this.src = src;
        this.dest = dest;
        this.offset = offset;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
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
        if (src instanceof VReg && vRegToSpill.containsKey(src) && dest instanceof VReg && vRegToSpill.containsKey(dest)) {
            int spill1 = vRegToSpill.get(src);
            int spill2 = vRegToSpill.get(dest);
            return List.of(new LdAsm(MReg.T2, MReg.SP, spill1), new LdAsm(MReg.T3, MReg.SP, spill2),
                    new SwAsm(MReg.T2, MReg.T3, offset));
        }
        if (src instanceof VReg && vRegToSpill.containsKey(src)) {
            int spill1 = vRegToSpill.get(src);
            return List.of(new LdAsm(MReg.T2, MReg.SP, spill1), new SwAsm(MReg.T2, dest, offset));
        }
        if (dest instanceof VReg && vRegToSpill.containsKey(dest)) {
            int spill2 = vRegToSpill.get(dest);
            return List.of(new LdAsm(MReg.T3, MReg.SP, spill2), new SwAsm(src, MReg.T3, offset));
        }
        return List.of(this);
    }

    @Override
    public String toString() {
        return String.format("sw %s,%d(%s)", src, offset, dest);
    }
}
