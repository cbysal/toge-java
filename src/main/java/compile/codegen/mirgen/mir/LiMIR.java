package compile.codegen.mirgen.mir;

import compile.codegen.MReg;
import compile.codegen.Reg;
import compile.codegen.VReg;

import java.util.List;
import java.util.Map;

public class LiMIR extends MIR {
    public final Reg dest;
    public final int imm;

    public LiMIR(Reg dest, int imm) {
        this.dest = dest;
        this.imm = imm;
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
        return new LiMIR(newDest, imm);
    }

    @Override
    public List<MIR> spill(Reg reg, int offset) {
        if (dest.equals(reg)) {
            VReg target = new VReg(reg.getType());
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
