package compile.codegen.mirgen.mir;

import compile.codegen.Label;
import compile.codegen.Reg;
import compile.codegen.virgen.VReg;
import compile.codegen.mirgen.MReg;

import java.util.List;
import java.util.Map;

public class BMIR implements MIR {
    public enum Op {
        EQ, NE, GE, GT, LE, LT
    }

    private final Op op;
    private Reg src1, src2;
    private final Label label;

    public BMIR(Label label) {
        this(null, null, null, label);
    }

    public BMIR(Op op, Reg src1, Reg src2, Label label) {
        this.op = op;
        this.src1 = src1;
        this.src2 = src2;
        this.label = label;
    }

    @Override
    public List<Reg> getRead() {
        if (src1 == null) {
            return List.of();
        }
        return List.of(src1, src2);
    }

    public List<Reg> getRegs() {
        if (src1 == null) {
            return List.of();
        }
        return List.of(src1, src2);
    }

    @Override
    public void replaceReg(Map<VReg, MReg> replaceMap) {
        if (src1 instanceof VReg && replaceMap.containsKey(src1))
            src1 = replaceMap.get(src1);
        if (src2 instanceof VReg && replaceMap.containsKey(src2))
            src2 = replaceMap.get(src2);
    }

    @Override
    public List<MIR> spill(Reg reg, int offset) {
        if (src1 == null || src2 == null) {
            return List.of(this);
        }
        if (src1.equals(reg) && src2.equals(reg)) {
            VReg src1 = new VReg(this.src1.getType());
            VReg src2 = new VReg(this.src2.getType());
            MIR ir1 = new LoadItemMIR(LoadItemMIR.Item.SPILL, src1, offset);
            MIR ir2 = new LoadItemMIR(LoadItemMIR.Item.SPILL, src2, offset);
            MIR ir3 = new BMIR(op, src1, src2, label);
            return List.of(ir1, ir2, ir3);
        }
        if (src1.equals(reg)) {
            VReg src1 = new VReg(this.src1.getType());
            MIR ir1 = new LoadItemMIR(LoadItemMIR.Item.SPILL, src1, offset);
            MIR ir2 = new BMIR(op, src1, src2, label);
            return List.of(ir1, ir2);
        }
        if (src2.equals(reg)) {
            VReg src2 = new VReg(this.src2.getType());
            MIR ir1 = new LoadItemMIR(LoadItemMIR.Item.SPILL, src2, offset);
            MIR ir2 = new BMIR(op, src1, src2, label);
            return List.of(ir1, ir2);
        }
        return List.of(this);
    }

    public boolean hasCond() {
        return op != null;
    }

    public Label getLabel() {
        return label;
    }

    @Override
    public String toString() {
        if (op == null) {
            return "j\t" + label;
        }
        return String.format("b%s\t%s,%s,%s", op.toString().toLowerCase(), src1, src2, label);
    }
}
