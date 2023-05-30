package compile.codegen.machine.asm;

import compile.codegen.machine.reg.MReg;
import compile.codegen.machine.reg.Reg;
import compile.codegen.machine.reg.VReg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record RrrAsm(Op op, Reg dest, Reg src1, Reg src2) implements Asm {
    public enum Op {
        ADD, SUB, MULW, DIV, REMW, SLT, SLTU, FADD, FSUB, FMUL, FDIV, FEQ, FLE, FLT
    }

    @Override
    public List<VReg> getVRegs() {
        List<VReg> vRegs = new ArrayList<>();
        if (dest instanceof VReg vReg) {
            vRegs.add(vReg);
        }
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
        if (dest instanceof VReg vDest && vRegToMReg.containsKey(vDest) && src1 instanceof VReg vSrc1 && vRegToMReg.containsKey(vSrc1) && src2 instanceof VReg vSrc2 && vRegToMReg.containsKey(vSrc2)) {
            return new RrrAsm(op, vRegToMReg.get(vDest), vRegToMReg.get(vSrc1), vRegToMReg.get(vSrc2));
        }
        if (dest instanceof VReg vDest && vRegToMReg.containsKey(vDest) && src1 instanceof VReg vSrc1 && vRegToMReg.containsKey(vSrc1)) {
            return new RrrAsm(op, vRegToMReg.get(vDest), vRegToMReg.get(vSrc1), src2);
        }
        if (dest instanceof VReg vDest && vRegToMReg.containsKey(vDest) && src2 instanceof VReg vSrc2 && vRegToMReg.containsKey(vSrc2)) {
            return new RrrAsm(op, vRegToMReg.get(vDest), src1, vRegToMReg.get(vSrc2));
        }
        if (src1 instanceof VReg vSrc1 && vRegToMReg.containsKey(vSrc1) && src2 instanceof VReg vSrc2 && vRegToMReg.containsKey(vSrc2)) {
            return new RrrAsm(op, dest, vRegToMReg.get(vSrc1), vRegToMReg.get(vSrc2));
        }
        if (dest instanceof VReg vDest && vRegToMReg.containsKey(vDest)) {
            return new RrrAsm(op, vRegToMReg.get(vDest), src1, src2);
        }
        if (src1 instanceof VReg vSrc1 && vRegToMReg.containsKey(vSrc1)) {
            return new RrrAsm(op, dest, vRegToMReg.get(vSrc1), src2);
        }
        if (src2 instanceof VReg vSrc2 && vRegToMReg.containsKey(vSrc2)) {
            return new RrrAsm(op, dest, src1, vRegToMReg.get(vSrc2));
        }
        return this;
    }

    @Override
    public List<Asm> spill(Map<VReg, Integer> vRegToSpill) {
        if (dest instanceof VReg && vRegToSpill.containsKey(dest) && src1 instanceof VReg && vRegToSpill.containsKey(src1) && src2 instanceof VReg && vRegToSpill.containsKey(src2)) {
            int spill1 = vRegToSpill.get(dest);
            int spill2 = vRegToSpill.get(src1);
            int spill3 = vRegToSpill.get(src2);
            return List.of(new LoadAsm(MReg.T2, MReg.SP, spill2, 8), new LoadAsm(MReg.T3, MReg.SP, spill3, 8), new RrrAsm(op, MReg.T2, MReg.T2, MReg.T3), new StoreAsm(MReg.T2, MReg.SP, spill1, 8));
        }
        if (dest instanceof VReg && vRegToSpill.containsKey(dest) && src1 instanceof VReg && vRegToSpill.containsKey(src1)) {
            int spill1 = vRegToSpill.get(dest);
            int spill2 = vRegToSpill.get(src1);
            return List.of(new LoadAsm(MReg.T2, MReg.SP, spill2, 8), new RrrAsm(op, MReg.T2, MReg.T2, src2), new StoreAsm(MReg.T2, MReg.SP, spill1, 8));
        }
        if (dest instanceof VReg && vRegToSpill.containsKey(dest) && src2 instanceof VReg && vRegToSpill.containsKey(src2)) {
            int spill1 = vRegToSpill.get(dest);
            int spill3 = vRegToSpill.get(src2);
            return List.of(new LoadAsm(MReg.T3, MReg.SP, spill3, 8), new RrrAsm(op, MReg.T2, src1, MReg.T3), new StoreAsm(MReg.T2, MReg.SP, spill1, 8));
        }
        if (src1 instanceof VReg && vRegToSpill.containsKey(src1) && src2 instanceof VReg && vRegToSpill.containsKey(src2)) {
            int spill2 = vRegToSpill.get(src1);
            int spill3 = vRegToSpill.get(src2);
            return List.of(new LoadAsm(MReg.T2, MReg.SP, spill2, 8), new LoadAsm(MReg.T3, MReg.SP, spill3, 8), new RrrAsm(op, dest, MReg.T2, MReg.T3));
        }
        if (dest instanceof VReg && vRegToSpill.containsKey(dest)) {
            int spill1 = vRegToSpill.get(dest);
            return List.of(new RrrAsm(op, MReg.T2, src1, src2), new StoreAsm(MReg.T2, MReg.SP, spill1, 8));
        }
        if (src1 instanceof VReg && vRegToSpill.containsKey(src1)) {
            int spill2 = vRegToSpill.get(src1);
            return List.of(new LoadAsm(MReg.T2, MReg.SP, spill2, 8), new RrrAsm(op, dest, MReg.T2, src2));
        }
        if (src2 instanceof VReg && vRegToSpill.containsKey(src2)) {
            int spill3 = vRegToSpill.get(src2);
            return List.of(new LoadAsm(MReg.T3, MReg.SP, spill3, 8), new RrrAsm(op, dest, src1, MReg.T3));
        }
        return List.of(this);
    }

    @Override
    public String toString() {
        return String.format("%s %s,%s,%s", switch (op) {
            case ADD, SUB, MULW, DIV, REMW, SLT, SLTU -> op.toString().toLowerCase();
            case FADD, FSUB, FMUL, FDIV, FEQ, FLE, FLT -> op.toString().toLowerCase() + ".s";
        }, dest, src1, src2);
    }
}
