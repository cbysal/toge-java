package compile.codegen.mirgen.mir;

import compile.codegen.Reg;
import compile.codegen.mirgen.MReg;
import compile.codegen.virgen.VReg;
import compile.symbol.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AddRegLocalMIR implements MIR {
    private Reg target;
    private final int imm;

    public AddRegLocalMIR(Reg target, int imm) {
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
        List<MIR> irs = new ArrayList<>();
        if (target.equals(reg)) {
            VReg target = new VReg(reg.getType(), reg.getSize());
            irs.add(new AddRegLocalMIR(target, imm));
            irs.add(new StoreItemMIR(StoreItemMIR.Item.SPILL, target, offset));
            return irs;
        }
        throw new RuntimeException();
    }

    public int getImm() {
        return imm;
    }

    @Override
    public String toString() {
        return "add\t" + target + ", $local, #" + imm;
    }
}
