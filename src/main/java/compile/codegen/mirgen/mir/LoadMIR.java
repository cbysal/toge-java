package compile.codegen.mirgen.mir;

import compile.codegen.MReg;
import compile.codegen.Reg;
import compile.codegen.VReg;
import compile.llvm.type.BasicType;

import java.util.List;
import java.util.Map;

public class LoadMIR extends MIR {
    public final Reg dest, src;
    public final int imm, size;

    public LoadMIR(Reg dest, Reg src, int imm, int size) {
        this.dest = dest;
        this.src = src;
        this.imm = imm;
        this.size = size;
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
        return new LoadMIR(newDest, newSrc, imm, size);
    }

    @Override
    public List<MIR> spill(Reg reg, int offset) {
        if (dest.equals(reg) && src.equals(reg)) {
            VReg target = new VReg(reg.getType());
            VReg source = new VReg(reg.getType());
            MIR ir1 = new LoadItemMIR(LoadItemMIR.Item.SPILL, source, offset);
            MIR ir2 = new LoadMIR(target, source, imm, size);
            MIR ir3 = new StoreItemMIR(StoreItemMIR.Item.SPILL, target, offset);
            return List.of(ir1, ir2, ir3);
        }
        if (dest.equals(reg)) {
            VReg target = new VReg(reg.getType());
            MIR ir1 = new LoadMIR(target, src, imm, size);
            MIR ir2 = new StoreItemMIR(StoreItemMIR.Item.SPILL, target, offset);
            return List.of(ir1, ir2);
        }
        if (src.equals(reg)) {
            VReg source = new VReg(reg.getType());
            MIR ir1 = new LoadItemMIR(LoadItemMIR.Item.SPILL, source, offset);
            MIR ir2 = new LoadMIR(dest, source, imm, size);
            return List.of(ir1, ir2);
        }
        return List.of(this);
    }

    @Override
    public String toString() {
        String str;
        if (dest.getType().equals(BasicType.FLOAT)) {
            str = "flw";
        } else if (dest.getType().equals(BasicType.I32)) {
            switch (size) {
                case 4:
                    str = "lw";
                    break;
                case 8:
                    str = "ld";
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + size);
            }
        } else {
            throw new IllegalStateException("Unexpected value: " + dest.getType());
        }
        return String.format("%s\t%s,%d(%s)", str, dest, imm, src);
    }
}
