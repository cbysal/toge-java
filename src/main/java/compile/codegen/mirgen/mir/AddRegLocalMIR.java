package compile.codegen.mirgen.mir;

import compile.codegen.Reg;
import compile.codegen.mirgen.MReg;
import compile.codegen.virgen.VReg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record AddRegLocalMIR(Reg dest, int imm) implements MIR {
    @Override
    public List<Reg> getWrite() {
        return List.of(dest);
    }

    @Override
    public MIR replaceReg(Map<VReg, MReg> replaceMap) {
        Reg newDest = dest;
        if (dest instanceof VReg && replaceMap.containsKey(dest))
            newDest = replaceMap.get(dest);
        return new AddRegLocalMIR(newDest, imm);
    }

    @Override
    public List<MIR> spill(Reg reg, int offset) {
        List<MIR> irs = new ArrayList<>();
        if (dest.equals(reg)) {
            VReg target = new VReg(reg.getType(), reg.getSize());
            irs.add(new AddRegLocalMIR(target, imm));
            irs.add(new StoreItemMIR(StoreItemMIR.Item.SPILL, target, offset));
            return irs;
        }
        throw new RuntimeException();
    }

    @Override
    public String toString() {
        return "add\t" + dest + ", $local, #" + imm;
    }
}
