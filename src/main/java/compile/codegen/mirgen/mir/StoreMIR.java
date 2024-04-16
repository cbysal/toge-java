package compile.codegen.mirgen.mir;

import compile.codegen.MReg;
import compile.codegen.Reg;
import compile.codegen.VReg;
import compile.llvm.type.BasicType;

import java.util.List;
import java.util.Map;

public class StoreMIR extends MIR {
    public final Reg src, dest;
    public final int imm, size;

    public StoreMIR(Reg src, Reg dest, int imm, int size) {
        this.src = src;
        this.dest = dest;
        this.imm = imm;
        this.size = size;
    }

    @Override
    public List<Reg> getRead() {
        return List.of(src, dest);
    }

    @Override
    public MIR replaceReg(Map<VReg, MReg> replaceMap) {
        Reg newSrc = src, newDest = dest;
        if (src instanceof VReg && replaceMap.containsKey(src))
            newSrc = replaceMap.get(src);
        if (dest instanceof VReg && replaceMap.containsKey(dest))
            newDest = replaceMap.get(dest);
        return new StoreMIR(newSrc, newDest, imm, size);
    }

    @Override
    public List<MIR> spill(Reg reg, int offset) {
        if (src.equals(reg) && dest.equals(reg)) {
            VReg source = new VReg(reg.getType());
            VReg target = new VReg(reg.getType());
            MIR ir1 = new LoadItemMIR(LoadItemMIR.Item.SPILL, source, offset);
            MIR ir2 = new LoadItemMIR(LoadItemMIR.Item.SPILL, target, offset);
            MIR ir3 = new StoreMIR(source, target, imm, size);
            return List.of(ir1, ir2, ir3);
        }
        if (src.equals(reg)) {
            VReg source = new VReg(reg.getType());
            MIR ir1 = new LoadItemMIR(LoadItemMIR.Item.SPILL, source, offset);
            MIR ir2 = new StoreMIR(source, dest, imm, size);
            return List.of(ir1, ir2);
        }
        if (dest.equals(reg)) {
            VReg target = new VReg(reg.getType());
            MIR ir1 = new LoadItemMIR(LoadItemMIR.Item.SPILL, target, offset);
            MIR ir2 = new StoreMIR(src, target, imm, size);
            return List.of(ir1, ir2);
        }
        return List.of(this);
    }

    @Override
    public String toString() {
        String s;
        if (src.getType().equals(BasicType.FLOAT)) {
            s = "fsw";
        } else if (src.getType().equals(BasicType.I32)) {
            switch (size) {
                case 4:
                    s = "sw";
                    break;
                case 8:
                    s = "sd";
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + size);
            }
        } else {
            throw new IllegalStateException("Unexpected value: " + src.getType());
        }
        return String.format("%s\t%s,%d(%s)", s, src, imm, dest);
    }
}
