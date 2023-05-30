package compile.codegen.machine.asm;

import compile.codegen.machine.Block;
import compile.codegen.machine.reg.MReg;
import compile.codegen.machine.reg.Reg;
import compile.codegen.machine.reg.VReg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record BAsm(Op op, Reg src1, Reg src2, Block dest) implements Asm {
    public enum Op {
        EQ, NE, GE, GT, LE, LT
    }

    @Override
    public List<VReg> getVRegs() {
        List<VReg> vRegs = new ArrayList<>();
        if (src1 instanceof VReg vReg) {
            vRegs.add(vReg);
        }
        if (src2 instanceof VReg vReg) {
            vRegs.add(vReg);
        }
        return vRegs;
    }

    @Override
    public Asm replaceVRegs(Map<VReg, MReg> vRegToMReg) {
        if (src1 instanceof VReg vSrc1 && vRegToMReg.containsKey(vSrc1) && src2 instanceof VReg vSrc2 && vRegToMReg.containsKey(vSrc2)) {
            return new BAsm(op, vRegToMReg.get(vSrc1), vRegToMReg.get(vSrc2), dest);
        }
        if (src1 instanceof VReg vSrc1 && vRegToMReg.containsKey(vSrc1)) {
            return new BAsm(op, vRegToMReg.get(vSrc1), src2, dest);
        }
        if (src2 instanceof VReg vSrc2 && vRegToMReg.containsKey(vSrc2)) {
            return new BAsm(op, src1, vRegToMReg.get(vSrc2), dest);
        }
        return this;
    }

    @Override
    public List<Asm> spill(Map<VReg, Integer> vRegToSpill) {
        if (src1 instanceof VReg && vRegToSpill.containsKey(src1) && src2 instanceof VReg && vRegToSpill.containsKey(src2)) {
            VReg newSrc1 = new VReg(false);
            VReg newSrc2 = new VReg(false);
            int spill1 = vRegToSpill.get(src1);
            int spill2 = vRegToSpill.get(src2);
            return List.of(new LoadAsm(newSrc1, MReg.SP, spill1, 8), new LoadAsm(newSrc2, MReg.SP, spill2, 8), new BAsm(op, newSrc1, newSrc2, dest));
        }
        if (src1 instanceof VReg && vRegToSpill.containsKey(src1)) {
            VReg newSrc1 = new VReg(false);
            int spill1 = vRegToSpill.get(src1);
            return List.of(new LoadAsm(newSrc1, MReg.SP, spill1, 8), new BAsm(op, newSrc1, src2, dest));
        }
        if (src2 instanceof VReg && vRegToSpill.containsKey(src2)) {
            VReg newSrc2 = new VReg(false);
            int spill2 = vRegToSpill.get(src2);
            return List.of(new LoadAsm(newSrc2, MReg.SP, spill2, 8), new BAsm(op, src1, newSrc2, dest));
        }
        return List.of(this);
    }

    @Override
    public String toString() {
        return String.format("b%s %s,%s,%s", op.toString().toLowerCase(), src1, src2, dest.getTag());
    }
}
