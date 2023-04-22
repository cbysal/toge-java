package codegen.machine.asm;

import codegen.machine.reg.MReg;
import codegen.machine.reg.Reg;
import codegen.machine.reg.VReg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RrrAsm implements Asm {
    public enum Type {
        ADD, SUB, MUL, DIV, REM, SLT, SLTU, FADD, FSUB, FMUL, FDIV, FEQ, FLE, FLT
    }

    private final Type type;
    private Reg dest, src1, src2;

    public RrrAsm(Type type, Reg dest, Reg src1, Reg src2) {
        this.type = type;
        this.dest = dest;
        this.src1 = src1;
        this.src2 = src2;
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
    public void replaceVRegs(Map<VReg, MReg> vRegToMReg) {
        if (dest instanceof VReg vReg && vRegToMReg.containsKey(vReg)) {
            dest = vRegToMReg.get(vReg);
        }
        if (src1 instanceof VReg vReg && vRegToMReg.containsKey(vReg)) {
            src1 = vRegToMReg.get(vReg);
        }
        if (src2 instanceof VReg vReg && vRegToMReg.containsKey(vReg)) {
            src2 = vRegToMReg.get(vReg);
        }
    }

    @Override
    public List<Asm> spill(Map<VReg, Integer> vRegToSpill) {
        if (dest instanceof VReg && vRegToSpill.containsKey(dest) && src1 instanceof VReg && vRegToSpill.containsKey(src1) && src2 instanceof VReg && vRegToSpill.containsKey(src2)) {
            int spill1 = vRegToSpill.get(dest);
            int spill2 = vRegToSpill.get(src1);
            int spill3 = vRegToSpill.get(src2);
            return List.of(new LdAsm(MReg.T2, MReg.SP, spill2), new LdAsm(MReg.T3, MReg.SP, spill3), new RrrAsm(type,
                    MReg.T2, MReg.T2, MReg.T3), new SdAsm(MReg.T2, MReg.SP, spill1));
        }
        if (dest instanceof VReg && vRegToSpill.containsKey(dest) && src1 instanceof VReg && vRegToSpill.containsKey(src1)) {
            int spill1 = vRegToSpill.get(dest);
            int spill2 = vRegToSpill.get(src1);
            return List.of(new LdAsm(MReg.T2, MReg.SP, spill2), new RrrAsm(type, MReg.T2, MReg.T2, src2),
                    new SdAsm(MReg.T2, MReg.SP, spill1));
        }
        if (dest instanceof VReg && vRegToSpill.containsKey(dest) && src2 instanceof VReg && vRegToSpill.containsKey(src2)) {
            int spill1 = vRegToSpill.get(dest);
            int spill3 = vRegToSpill.get(src2);
            return List.of(new LdAsm(MReg.T3, MReg.SP, spill3), new RrrAsm(type, MReg.T2, src1, MReg.T3),
                    new SdAsm(MReg.T2, MReg.SP, spill1));
        }
        if (src1 instanceof VReg && vRegToSpill.containsKey(src1) && src2 instanceof VReg && vRegToSpill.containsKey(src2)) {
            int spill2 = vRegToSpill.get(src1);
            int spill3 = vRegToSpill.get(src2);
            return List.of(new LdAsm(MReg.T2, MReg.SP, spill2), new LdAsm(MReg.T3, MReg.SP, spill3), new RrrAsm(type,
                    dest, MReg.T2, MReg.T3));
        }
        if (dest instanceof VReg && vRegToSpill.containsKey(dest)) {
            int spill1 = vRegToSpill.get(dest);
            return List.of(new RrrAsm(type, MReg.T2, src1, src2), new SdAsm(MReg.T2, MReg.SP, spill1));
        }
        if (src1 instanceof VReg && vRegToSpill.containsKey(src1)) {
            int spill2 = vRegToSpill.get(src1);
            return List.of(new LdAsm(MReg.T2, MReg.SP, spill2), new RrrAsm(type, dest, MReg.T2, src2));
        }
        if (src2 instanceof VReg && vRegToSpill.containsKey(src2)) {
            int spill3 = vRegToSpill.get(src2);
            return List.of(new LdAsm(MReg.T3, MReg.SP, spill3), new RrrAsm(type, dest, src1, MReg.T3));
        }
        return List.of(this);
    }

    @Override
    public String toString() {
        return String.format("%s %s,%s,%s", switch (type) {
            case ADD, SUB, MUL, DIV, REM, SLT, SLTU -> type.toString().toLowerCase();
            case FADD, FSUB, FMUL, FDIV, FEQ, FLE, FLT -> type.toString().toLowerCase() + ".s";
        }, dest, src1, src2);
    }
}
