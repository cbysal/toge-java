package compile.codegen.mirgen.mir;

import compile.codegen.Reg;
import compile.codegen.mirgen.MReg;
import compile.codegen.virgen.VReg;

import java.util.List;
import java.util.Map;

public class StoreItemMIR extends MIR {
    public final Item item;
    public final Reg src;
    public final int imm;
    public StoreItemMIR(Item item, Reg src, int imm) {
        this.item = item;
        this.src = src;
        this.imm = imm;
    }

    @Override
    public List<Reg> getRead() {
        return List.of(src);
    }

    @Override
    public MIR replaceReg(Map<VReg, MReg> replaceMap) {
        Reg newSrc = src;
        if (src instanceof VReg && replaceMap.containsKey(src))
            newSrc = replaceMap.get(src);
        return new StoreItemMIR(item, newSrc, imm);
    }

    @Override
    public List<MIR> spill(Reg reg, int offset) {
        if (src.equals(reg)) {
            VReg source = new VReg(reg.getType(), reg.getSize());
            MIR ir1 = new LoadItemMIR(LoadItemMIR.Item.SPILL, source, offset);
            MIR ir2 = new StoreItemMIR(item, source, imm);
            return List.of(ir1, ir2);
        }
        return List.of(this);
    }

    @Override
    public String toString() {
        return String.format("store\t%s,%d($%s)", src, imm, item.toString().toLowerCase());
    }

    public enum Item {
        LOCAL, PARAM_CALL, PARAM_INNER, PARAM_OUTER, SPILL
    }
}
