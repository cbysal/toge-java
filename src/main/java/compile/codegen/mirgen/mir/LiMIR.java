package compile.codegen.mirgen.mir;

import compile.codegen.Reg;
import compile.codegen.mirgen.MReg;
import compile.codegen.virgen.VReg;

import java.util.List;
import java.util.Map;

public record LiMIR(Reg dest, int imm) implements MIR {
    @Override
    public List<Reg> getWrite() {
        return List.of(dest);
    }

    @Override
    public MIR replaceReg(Map<VReg, MReg> replaceMap) {
        Reg newDest = dest;
        if (dest instanceof VReg && replaceMap.containsKey(dest))
            newDest = replaceMap.get(dest);
        return new LiMIR(newDest, imm);
    }

    @Override
    public List<MIR> spill(Reg reg, int offset) {
        if (dest.equals(reg)) {
            VReg target = new VReg(reg.getType(), reg.getSize());
            MIR ir1 = new LiMIR(target, imm);
            MIR ir2 = new StoreItemMIR(StoreItemMIR.Item.SPILL, target, offset);
            return List.of(ir1, ir2);
        }
        return List.of(this);
    }

    @Override
    public String toString() {
        return "li\t" + dest + ", " + imm;
    }
}
