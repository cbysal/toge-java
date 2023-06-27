package compile.codegen.mirgen.mir;

import compile.codegen.Reg;
import compile.codegen.mirgen.MReg;
import compile.codegen.virgen.VReg;
import compile.symbol.Type;

import java.util.List;
import java.util.Map;

public class RriMIR implements MIR {
    public enum Op {
        ADDI, SLTI, SLTIU
    }

    private final Op op;
    private Reg target, source;
    private final int imm;

    public RriMIR(Op op, Reg target, Reg source, int imm) {
        this.op = op;
        this.target = target;
        this.source = source;
        this.imm = imm;
    }

    @Override
    public List<Reg> getRegs() {
        return List.of(target, source);
    }

    @Override
    public List<Reg> getRead() {
        return List.of(source);
    }

    @Override
    public List<Reg> getWrite() {
        return List.of(target);
    }

    @Override
    public void replaceReg(Map<VReg, MReg> replaceMap) {
        if (target instanceof VReg && replaceMap.containsKey(target))
            target = replaceMap.get(target);
        if (source instanceof VReg && replaceMap.containsKey(source))
            source = replaceMap.get(source);
    }

    @Override
    public List<MIR> spill(Reg reg, int offset) {
        if (target.equals(reg) && source.equals(reg)) {
            VReg target = new VReg(Type.INT);
            VReg source = new VReg(Type.INT);
            MIR ir1 = new LoadItemMIR(LoadItemMIR.Item.SPILL, source, offset);
            MIR ir2 = new RriMIR(op, target, source, imm);
            MIR ir3 = new StoreItemMIR(StoreItemMIR.Item.SPILL, target, offset);
            return List.of(ir1, ir2, ir3);
        }
        if (target.equals(reg)) {
            VReg target = new VReg(Type.INT);
            MIR ir1 = new RriMIR(op, target, source, imm);
            MIR ir2 = new StoreItemMIR(StoreItemMIR.Item.SPILL, target, offset);
            return List.of(ir1, ir2);
        }
        if (source.equals(reg)) {
            VReg source = new VReg(Type.INT);
            MIR ir1 = new LoadItemMIR(LoadItemMIR.Item.SPILL, source, offset);
            MIR ir2 = new RriMIR(op, target, source, imm);
            return List.of(ir1, ir2);
        }
        return List.of(this);
    }

    @Override
    public String toString() {
        return String.format("%s\t%s,%s,%d", op.toString().toLowerCase(), target, source, imm);
    }
}
