package compile.codegen.mirgen.mir;

import compile.codegen.Reg;
import compile.codegen.mirgen.MReg;
import compile.codegen.virgen.VReg;

import java.util.List;
import java.util.Map;

public class RrrMIR implements MIR {
    public enum Op {
        ADD, ADDW, SUB, SUBW, MUL, DIV, REM, EQ, GE, GT, LE, LT, XOR
    }

    private final Op op;
    private Reg target, source1, source2;

    public RrrMIR(Op op, Reg target, Reg source1, Reg source2) {
        this.op = op;
        this.target = target;
        this.source1 = source1;
        this.source2 = source2;
    }

    @Override
    public List<Reg> getRegs() {
        return List.of(target, source1, source2);
    }

    @Override
    public List<Reg> getRead() {
        return List.of(source1, source2);
    }

    @Override
    public List<Reg> getWrite() {
        return List.of(target);
    }

    @Override
    public void replaceReg(Map<VReg, MReg> replaceMap) {
        if (target instanceof VReg && replaceMap.containsKey(target))
            target = replaceMap.get(target);
        if (source1 instanceof VReg && replaceMap.containsKey(source1))
            source1 = replaceMap.get(source1);
        if (source2 instanceof VReg && replaceMap.containsKey(source2))
            source2 = replaceMap.get(source2);
    }

    @Override
    public List<MIR> spill(Reg reg, int offset) {
        if (target.equals(reg) && source1.equals(reg) && source2.equals(reg)) {
            VReg target = new VReg(reg.getType(), reg.getSize());
            VReg source1 = new VReg(reg.getType(), reg.getSize());
            VReg source2 = new VReg(reg.getType(), reg.getSize());
            MIR ir1 = new LoadItemMIR(LoadItemMIR.Item.SPILL, source1, offset);
            MIR ir2 = new LoadItemMIR(LoadItemMIR.Item.SPILL, source1, offset);
            MIR ir3 = new RrrMIR(op, target, source1, source2);
            MIR ir4 = new StoreItemMIR(StoreItemMIR.Item.SPILL, target, offset);
            return List.of(ir1, ir2, ir3, ir4);
        }
        if (target.equals(reg) && source1.equals(reg)) {
            VReg target = new VReg(reg.getType(), reg.getSize());
            VReg source1 = new VReg(reg.getType(), reg.getSize());
            MIR ir1 = new LoadItemMIR(LoadItemMIR.Item.SPILL, source1, offset);
            MIR ir2 = new RrrMIR(op, target, source1, source2);
            MIR ir3 = new StoreItemMIR(StoreItemMIR.Item.SPILL, target, offset);
            return List.of(ir1, ir2, ir3);
        }
        if (target.equals(reg) && source2.equals(reg)) {
            VReg target = new VReg(reg.getType(), reg.getSize());
            VReg source2 = new VReg(reg.getType(), reg.getSize());
            MIR ir1 = new LoadItemMIR(LoadItemMIR.Item.SPILL, source2, offset);
            MIR ir2 = new RrrMIR(op, target, source1, source2);
            MIR ir3 = new StoreItemMIR(StoreItemMIR.Item.SPILL, target, offset);
            return List.of(ir1, ir2, ir3);
        }
        if (source1.equals(reg) && source2.equals(reg)) {
            VReg source1 = new VReg(reg.getType(), reg.getSize());
            VReg source2 = new VReg(reg.getType(), reg.getSize());
            MIR ir1 = new LoadItemMIR(LoadItemMIR.Item.SPILL, source1, offset);
            MIR ir2 = new LoadItemMIR(LoadItemMIR.Item.SPILL, source2, offset);
            MIR ir3 = new RrrMIR(op, target, source1, source2);
            return List.of(ir1, ir2, ir3);
        }
        if (target.equals(reg)) {
            VReg target = new VReg(reg.getType(), reg.getSize());
            MIR ir1 = new RrrMIR(op, target, source1, source2);
            MIR ir2 = new StoreItemMIR(StoreItemMIR.Item.SPILL, target, offset);
            return List.of(ir1, ir2);
        }
        if (source1.equals(reg)) {
            VReg source1 = new VReg(reg.getType(), reg.getSize());
            MIR ir1 = new LoadItemMIR(LoadItemMIR.Item.SPILL, source1, offset);
            MIR ir2 = new RrrMIR(op, target, source1, source2);
            return List.of(ir1, ir2);
        }
        if (source2.equals(reg)) {
            VReg source2 = new VReg(reg.getType(), reg.getSize());
            MIR ir1 = new LoadItemMIR(LoadItemMIR.Item.SPILL, source2, offset);
            MIR ir2 = new RrrMIR(op, target, source1, source2);
            return List.of(ir1, ir2);
        }
        return List.of(this);
    }

    @Override
    public String toString() {
        return String.format("%s\t%s, %s, %s", switch (target.getType()) {
            case FLOAT -> switch (op) {
                case ADD, SUB, MUL, DIV -> String.format("f%s.s", op.toString().toLowerCase());
                default -> throw new IllegalStateException("Unexpected value: " + op);
            };
            case INT -> switch (op) {
                case ADD, ADDW, SUB, SUBW, MUL, DIV, XOR -> op.toString().toLowerCase();
                case REM -> String.format("%sw", op.toString().toLowerCase());
                case EQ, GE, GT, LE, LT -> String.format("f%s.s", op.toString().toLowerCase());
            };
            default -> throw new IllegalStateException("Unexpected value: " + target.getType());
        }, target, source1, source2);
    }
}
