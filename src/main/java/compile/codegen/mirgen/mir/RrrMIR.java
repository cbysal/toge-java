package compile.codegen.mirgen.mir;

import compile.codegen.MReg;
import compile.codegen.Reg;
import compile.codegen.VReg;
import compile.llvm.type.BasicType;

import java.util.List;
import java.util.Map;

public class RrrMIR extends MIR {
    public final Op op;
    public final Reg dest, src1, src2;

    public RrrMIR(Op op, Reg dest, Reg src1, Reg src2) {
        this.op = op;
        this.dest = dest;
        this.src1 = src1;
        this.src2 = src2;
    }

    @Override
    public List<Reg> getRead() {
        return List.of(src1, src2);
    }

    @Override
    public List<Reg> getWrite() {
        return List.of(dest);
    }

    @Override
    public MIR replaceReg(Map<VReg, MReg> replaceMap) {
        Reg newDest = dest, newSrc1 = src1, newSrc2 = src2;
        if (dest instanceof VReg && replaceMap.containsKey(dest))
            newDest = replaceMap.get(dest);
        if (src1 instanceof VReg && replaceMap.containsKey(src1))
            newSrc1 = replaceMap.get(src1);
        if (src2 instanceof VReg && replaceMap.containsKey(src2))
            newSrc2 = replaceMap.get(src2);
        return new RrrMIR(op, newDest, newSrc1, newSrc2);
    }

    @Override
    public List<MIR> spill(Reg reg, int offset) {
        if (dest.equals(reg) && src1.equals(reg) && src2.equals(reg)) {
            VReg target = new VReg(reg.getType());
            VReg source1 = new VReg(reg.getType());
            VReg source2 = new VReg(reg.getType());
            MIR ir1 = new LoadItemMIR(LoadItemMIR.Item.SPILL, source1, offset);
            MIR ir2 = new LoadItemMIR(LoadItemMIR.Item.SPILL, source1, offset);
            MIR ir3 = new RrrMIR(op, target, source1, source2);
            MIR ir4 = new StoreItemMIR(StoreItemMIR.Item.SPILL, target, offset);
            return List.of(ir1, ir2, ir3, ir4);
        }
        if (dest.equals(reg) && src1.equals(reg)) {
            VReg target = new VReg(reg.getType());
            VReg source1 = new VReg(reg.getType());
            MIR ir1 = new LoadItemMIR(LoadItemMIR.Item.SPILL, source1, offset);
            MIR ir2 = new RrrMIR(op, target, source1, src2);
            MIR ir3 = new StoreItemMIR(StoreItemMIR.Item.SPILL, target, offset);
            return List.of(ir1, ir2, ir3);
        }
        if (dest.equals(reg) && src2.equals(reg)) {
            VReg target = new VReg(reg.getType());
            VReg source2 = new VReg(reg.getType());
            MIR ir1 = new LoadItemMIR(LoadItemMIR.Item.SPILL, source2, offset);
            MIR ir2 = new RrrMIR(op, target, src1, source2);
            MIR ir3 = new StoreItemMIR(StoreItemMIR.Item.SPILL, target, offset);
            return List.of(ir1, ir2, ir3);
        }
        if (src1.equals(reg) && src2.equals(reg)) {
            VReg source1 = new VReg(reg.getType());
            VReg source2 = new VReg(reg.getType());
            MIR ir1 = new LoadItemMIR(LoadItemMIR.Item.SPILL, source1, offset);
            MIR ir2 = new LoadItemMIR(LoadItemMIR.Item.SPILL, source2, offset);
            MIR ir3 = new RrrMIR(op, dest, source1, source2);
            return List.of(ir1, ir2, ir3);
        }
        if (dest.equals(reg)) {
            VReg target = new VReg(reg.getType());
            MIR ir1 = new RrrMIR(op, target, src1, src2);
            MIR ir2 = new StoreItemMIR(StoreItemMIR.Item.SPILL, target, offset);
            return List.of(ir1, ir2);
        }
        if (src1.equals(reg)) {
            VReg source1 = new VReg(reg.getType());
            MIR ir1 = new LoadItemMIR(LoadItemMIR.Item.SPILL, source1, offset);
            MIR ir2 = new RrrMIR(op, dest, source1, src2);
            return List.of(ir1, ir2);
        }
        if (src2.equals(reg)) {
            VReg source2 = new VReg(reg.getType());
            MIR ir1 = new LoadItemMIR(LoadItemMIR.Item.SPILL, source2, offset);
            MIR ir2 = new RrrMIR(op, dest, src1, source2);
            return List.of(ir1, ir2);
        }
        return List.of(this);
    }

    @Override
    public String toString() {
        return String.format("%s\t%s, %s, %s", switch (dest.getType()) {
            case BasicType.FLOAT -> switch (op) {
                case ADD, SUB, MUL, DIV -> String.format("f%s.s", op.toString().toLowerCase());
                default -> throw new IllegalStateException("Unexpected value: " + op);
            };
            case BasicType.I32 -> switch (op) {
                case ADD, ADDW, SUB, SUBW, MUL, MULW, DIV, DIVW, REMW, XOR, AND, SLT, SGT ->
                        op.toString().toLowerCase();
                case EQ, GE, GT, LE, LT -> String.format("f%s.s", op.toString().toLowerCase());
            };
            default -> throw new IllegalStateException("Unexpected value: " + dest.getType());
        }, dest, src1, src2);
    }

    public enum Op {
        ADD, ADDW, SUB, SUBW, MUL, MULW, DIV, DIVW, REMW, EQ, GE, GT, LE, LT, AND, XOR, SLT, SGT
    }
}
