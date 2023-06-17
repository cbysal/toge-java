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
            VReg newDest = new VReg(dest.isFloat());
            VReg newSrc1 = new VReg(src1.isFloat());
            VReg newSrc2 = new VReg(src2.isFloat());
            int spill1 = vRegToSpill.get(dest);
            int spill2 = vRegToSpill.get(src1);
            int spill3 = vRegToSpill.get(src2);
            return List.of(new VLoadSpillAsm(newSrc1, spill2), new VLoadSpillAsm(newSrc2, spill3), new RrrAsm(op, newDest, newSrc1, newSrc2), new VStoreSpillAsm(newDest, spill1));
        }
        if (dest instanceof VReg && vRegToSpill.containsKey(dest) && src1 instanceof VReg && vRegToSpill.containsKey(src1)) {
            VReg newDest = new VReg(dest.isFloat());
            VReg newSrc1 = new VReg(src1.isFloat());
            int spill1 = vRegToSpill.get(dest);
            int spill2 = vRegToSpill.get(src1);
            return List.of(new VLoadSpillAsm(newSrc1, spill2), new RrrAsm(op, newDest, newSrc1, src2), new VStoreSpillAsm(newDest, spill1));
        }
        if (dest instanceof VReg && vRegToSpill.containsKey(dest) && src2 instanceof VReg && vRegToSpill.containsKey(src2)) {
            VReg newDest = new VReg(dest.isFloat());
            VReg newSrc2 = new VReg(src2.isFloat());
            int spill1 = vRegToSpill.get(dest);
            int spill2 = vRegToSpill.get(src2);
            return List.of(new VLoadSpillAsm(newSrc2, spill2), new RrrAsm(op, newDest, src1, newSrc2), new VStoreSpillAsm(newDest, spill1));
        }
        if (src1 instanceof VReg && vRegToSpill.containsKey(src1) && src2 instanceof VReg && vRegToSpill.containsKey(src2)) {
            VReg newSrc1 = new VReg(src1.isFloat());
            VReg newSrc2 = new VReg(src2.isFloat());
            int spill1 = vRegToSpill.get(src1);
            int spill2 = vRegToSpill.get(src2);
            return List.of(new VLoadSpillAsm(newSrc1, spill1), new VLoadSpillAsm(newSrc2, spill2), new RrrAsm(op, dest, newSrc1, newSrc2));
        }
        if (dest instanceof VReg && vRegToSpill.containsKey(dest)) {
            VReg newDest = new VReg(dest.isFloat());
            int spill = vRegToSpill.get(dest);
            return List.of(new RrrAsm(op, newDest, src1, src2), new VStoreSpillAsm(newDest, spill));
        }
        if (src1 instanceof VReg && vRegToSpill.containsKey(src1)) {
            VReg newSrc1 = new VReg(src1.isFloat());
            int spill = vRegToSpill.get(src1);
            return List.of(new VLoadSpillAsm(newSrc1, spill), new RrrAsm(op, dest, newSrc1, src2));
        }
        if (src2 instanceof VReg && vRegToSpill.containsKey(src2)) {
            VReg newSrc2 = new VReg(src2.isFloat());
            int spill = vRegToSpill.get(src2);
            return List.of(new VLoadSpillAsm(newSrc2, spill), new RrrAsm(op, dest, src1, newSrc2));
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
