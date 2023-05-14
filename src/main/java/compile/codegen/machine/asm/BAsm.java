package compile.codegen.machine.asm;

import compile.codegen.machine.Block;
import compile.codegen.machine.reg.MReg;
import compile.codegen.machine.reg.Reg;
import compile.codegen.machine.reg.VReg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BAsm implements Asm {
    public enum Op {
        EQ, NE, GE, GT, LE, LT
    }

    private final Op op;
    private Reg src1, src2;
    private final Block dest;

    public BAsm(Op op, Reg src1, Reg src2, Block dest) {
        this.op = op;
        this.src1 = src1;
        this.src2 = src2;
        this.dest = dest;
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
    public void replaceVRegs(Map<VReg, MReg> vRegToMReg) {
        if (src1 instanceof VReg vReg && vRegToMReg.containsKey(vReg)) {
            src1 = vRegToMReg.get(vReg);
        }
        if (src2 instanceof VReg vReg && vRegToMReg.containsKey(vReg)) {
            src2 = vRegToMReg.get(vReg);
        }
    }

    @Override
    public List<Asm> spill(Map<VReg, Integer> vRegToSpill) {
        if (src1 instanceof VReg && vRegToSpill.containsKey(src1) && src2 instanceof VReg && vRegToSpill.containsKey(src2)) {
            int spill1 = vRegToSpill.get(src1);
            int spill2 = vRegToSpill.get(src2);
            return List.of(new LdAsm(MReg.T2, MReg.SP, spill1), new LdAsm(MReg.T3, MReg.SP, spill2), new BAsm(op,
                    MReg.T2, MReg.T3, dest));
        }
        if (src1 instanceof VReg && vRegToSpill.containsKey(src1)) {
            int spill1 = vRegToSpill.get(src1);
            return List.of(new LdAsm(MReg.T2, MReg.SP, spill1), new BAsm(op, MReg.T2, src2, dest));
        }
        if (src2 instanceof VReg && vRegToSpill.containsKey(src2)) {
            int spill2 = vRegToSpill.get(src2);
            return List.of(new LdAsm(MReg.T3, MReg.SP, spill2), new BAsm(op, src1, MReg.T3, dest));
        }
        return List.of(this);
    }

    @Override
    public String toString() {
        return String.format("b%s %s,%s,%s", op.toString().toLowerCase(), src1, src2, dest.getTag());
    }
}
