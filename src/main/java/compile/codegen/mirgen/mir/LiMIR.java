package compile.codegen.mirgen.mir;

import compile.codegen.Reg;
import compile.codegen.mirgen.MReg;
import compile.codegen.virgen.VReg;
import compile.symbol.Type;

import java.util.List;
import java.util.Map;

public class LiMIR implements MIR {
    private Reg target;
    private final int imm;

    public LiMIR(Reg target, int imm) {
        this.target = target;
        this.imm = imm;
    }

    @Override
    public List<Reg> getRegs() {
        return List.of(target);
    }

    public Reg getTarget() {
        return target;
    }

    @Override
    public List<Reg> getWrite() {
        return List.of(target);
    }

    @Override
    public void replaceReg(Map<VReg, MReg> replaceMap) {
        if (target instanceof VReg && replaceMap.containsKey(target))
            target = replaceMap.get(target);
    }

    @Override
    public List<MIR> spill(Reg reg, int offset) {
        if (target.equals(reg)) {
            VReg target = new VReg(Type.INT);
            MIR ir1 = new LiMIR(target, imm);
            MIR ir2 = new StoreItemMIR(StoreItemMIR.Item.SPILL, target, offset);
            return List.of(ir1, ir2);
        }
        return List.of(this);
    }

    public int getImm() {
        return imm;
    }

    @Override
    public String toString() {
        return "li\t" + target + ", " + imm;
    }
}
