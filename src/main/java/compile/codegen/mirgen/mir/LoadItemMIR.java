package compile.codegen.mirgen.mir;

import compile.codegen.Reg;
import compile.codegen.mirgen.MReg;
import compile.codegen.virgen.VReg;

import java.util.List;
import java.util.Map;

public record LoadItemMIR(Item item, Reg dest, int imm) implements MIR {
    public enum Item {
        SPILL, PARAM_INNER, PARAM_OUTER, LOCAL
    }

    @Override
    public List<Reg> getWrite() {
        return List.of(dest);
    }

    @Override
    public MIR replaceReg(Map<VReg, MReg> replaceMap) {
        Reg newDest = dest;
        if (dest instanceof VReg && replaceMap.containsKey(dest))
            newDest = replaceMap.get(dest);
        return new LoadItemMIR(item, newDest, imm);
    }

    @Override
    public List<MIR> spill(Reg reg, int offset) {
        if (dest.equals(reg)) {
            VReg target = new VReg(reg.getType(), reg.getSize());
            MIR ir1 = new LoadItemMIR(item, target, imm);
            MIR ir2 = new StoreItemMIR(StoreItemMIR.Item.SPILL, target, offset);
            return List.of(ir1, ir2);
        }
        return List.of(this);
    }

    @Override
    public String toString() {
        return String.format("load\t%s,%d($%s)", dest, imm, item);
    }
}
