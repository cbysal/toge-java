package codegen.machine.asm;

import codegen.machine.reg.MReg;
import codegen.machine.reg.Reg;
import codegen.machine.reg.VReg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FswAsm implements Asm {
    private Reg src, dest;
    private int offset;

    public FswAsm(Reg src, Reg dest) {
        this(src, dest, 0);
    }

    public FswAsm(Reg src, Reg dest, int offset) {
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
        return List.of(this);
    }

    @Override
    public String toString() {
        return String.format("fsw %s,%d(%s)", src, offset, dest);
    }
}
