package compile.codegen.mirgen.mir;

import compile.codegen.Reg;
import compile.codegen.mirgen.MReg;
import compile.codegen.virgen.VReg;

import java.util.List;
import java.util.Map;

public record RriMIR(Op op, Reg dest, Reg src, int imm) implements MIR {
    public enum Op {
        ADDI, SLTI, SLTIU, SLLIW, SRAI, SRAIW, SRLI, SRLIW
    }

    @Override
    public List<Reg> getRead() {
        return List.of(src);
    }

    @Override
    public List<Reg> getWrite() {
        return List.of(dest);
    }

    @Override
    public MIR replaceReg(Map<VReg, MReg> replaceMap) {
        Reg newDest = dest, newSrc = src;
        if (dest instanceof VReg && replaceMap.containsKey(dest))
            newDest = replaceMap.get(dest);
        if (src instanceof VReg && replaceMap.containsKey(src))
            newSrc = replaceMap.get(src);
        return new RriMIR(op, newDest, newSrc, imm);
    }

    @Override
    public List<MIR> spill(Reg reg, int offset) {
        if (dest.equals(reg) && src.equals(reg)) {
            VReg target = new VReg(reg.getType(), reg.getSize());
            VReg source = new VReg(reg.getType(), reg.getSize());
            MIR ir1 = new LoadItemMIR(LoadItemMIR.Item.SPILL, source, offset);
            MIR ir2 = new RriMIR(op, target, source, imm);
            MIR ir3 = new StoreItemMIR(StoreItemMIR.Item.SPILL, target, offset);
            return List.of(ir1, ir2, ir3);
        }
        if (dest.equals(reg)) {
            VReg target = new VReg(reg.getType(), reg.getSize());
            MIR ir1 = new RriMIR(op, target, src, imm);
            MIR ir2 = new StoreItemMIR(StoreItemMIR.Item.SPILL, target, offset);
            return List.of(ir1, ir2);
        }
        if (src.equals(reg)) {
            VReg source = new VReg(reg.getType(), reg.getSize());
            MIR ir1 = new LoadItemMIR(LoadItemMIR.Item.SPILL, source, offset);
            MIR ir2 = new RriMIR(op, dest, source, imm);
            return List.of(ir1, ir2);
        }
        return List.of(this);
    }

    @Override
    public String toString() {
        return String.format("%s\t%s,%s,%d", op.toString().toLowerCase(), dest, src, imm);
    }
}
