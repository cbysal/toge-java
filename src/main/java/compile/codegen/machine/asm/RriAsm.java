package compile.codegen.machine.asm;

import compile.codegen.machine.reg.MReg;
import compile.codegen.machine.reg.Reg;
import compile.codegen.machine.reg.VReg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record RriAsm(Op op, Reg dest, Reg src, int imm) implements Asm {
    public enum Op {
        ADD, SUB, SLTI, SLTIU, XORI
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
    public Asm replaceVRegs(Map<VReg, MReg> vRegToMReg) {
        if (dest instanceof VReg vDest && vRegToMReg.containsKey(vDest) && src instanceof VReg vSrc && vRegToMReg.containsKey(vSrc)) {
            return new RriAsm(op, vRegToMReg.get(vDest), vRegToMReg.get(vSrc), imm);
        }
        if (dest instanceof VReg vDest && vRegToMReg.containsKey(vDest)) {
            return new RriAsm(op, vRegToMReg.get(vDest), src, imm);
        }
        if (src instanceof VReg vSrc && vRegToMReg.containsKey(vSrc)) {
            return new RriAsm(op, dest, vRegToMReg.get(vSrc), imm);
        }
        return this;
    }

    @Override
    public List<Asm> spill(Map<VReg, Integer> vRegToSpill) {
        if (dest instanceof VReg && vRegToSpill.containsKey(dest) && src instanceof VReg && vRegToSpill.containsKey(src)) {
            int spill1 = vRegToSpill.get(dest);
            int spill2 = vRegToSpill.get(src);
            return List.of(new LoadAsm(MReg.T2, MReg.SP, spill2, 8), new RriAsm(op, MReg.T2, MReg.T2, imm), new StoreAsm(MReg.T2, MReg.SP, spill1, 8));
        }
        if (dest instanceof VReg && vRegToSpill.containsKey(dest)) {
            int spill1 = vRegToSpill.get(dest);
            return List.of(new RriAsm(op, MReg.T2, src, imm), new StoreAsm(MReg.T2, MReg.SP, spill1, 8));
        }
        if (src instanceof VReg && vRegToSpill.containsKey(src)) {
            int spill2 = vRegToSpill.get(src);
            return List.of(new LoadAsm(MReg.T2, MReg.SP, spill2, 8), new RriAsm(op, dest, MReg.T2, imm));
        }
        return List.of(this);
    }

    @Override
    public String toString() {
        return String.format("%s %s,%s,%d", op.toString().toLowerCase(), dest, src, imm);
    }
}
