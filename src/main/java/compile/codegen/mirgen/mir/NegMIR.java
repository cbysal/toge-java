package compile.codegen.mirgen.mir;

import compile.codegen.Reg;
import compile.codegen.mirgen.MReg;
import compile.codegen.virgen.VReg;
import compile.symbol.Type;

import java.util.List;
import java.util.Map;

public class NegMIR implements MIR {
    private Reg target, source;

    public NegMIR(Reg target, Reg source) {
        this.target = target;
        this.source = source;
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
            VReg target = new VReg(reg.getType(), reg.getSize());
            VReg source = new VReg(reg.getType(), reg.getSize());
            MIR ir1 = new LoadItemMIR(LoadItemMIR.Item.SPILL, source, offset);
            MIR ir2 = new MvMIR(target, source);
            MIR ir3 = new StoreItemMIR(StoreItemMIR.Item.SPILL, target, offset);
            return List.of(ir1, ir2, ir3);
        }
        if (target.equals(reg)) {
            VReg target = new VReg(reg.getType(), reg.getSize());
            MIR ir1 = new MvMIR(target, source);
            MIR ir2 = new StoreItemMIR(StoreItemMIR.Item.SPILL, target, offset);
            return List.of(ir1, ir2);
        }
        if (source.equals(reg)) {
            VReg source = new VReg(reg.getType(), reg.getSize());
            MIR ir1 = new LoadItemMIR(LoadItemMIR.Item.SPILL, source, offset);
            MIR ir2 = new MvMIR(target, source);
            return List.of(ir1, ir2);
        }
        return List.of(this);
    }

    @Override
    public String toString() {
        return "fneg.s\t" + target + ", " + source;
    }
}
