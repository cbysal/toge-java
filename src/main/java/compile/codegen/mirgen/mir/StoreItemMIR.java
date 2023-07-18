package compile.codegen.mirgen.mir;

import compile.codegen.Reg;
import compile.codegen.mirgen.MReg;
import compile.codegen.virgen.VReg;
import compile.symbol.Type;

import java.util.List;
import java.util.Map;

public class StoreItemMIR implements MIR {
    public enum Item {
        LOCAL, PARAM_CALL, PARAM_INNER, PARAM_OUTER, SPILL
    }

    private final Item item;
    private Reg src;
    private final int imm;

    public StoreItemMIR(Item item, Reg src, int imm) {
        this.item = item;
        this.src = src;
        this.imm = imm;
    }

    @Override
    public List<Reg> getRegs() {
        return List.of(src);
    }

    @Override
    public List<Reg> getRead() {
        return List.of(src);
    }

    public Reg getSrc() {
        return src;
    }

    @Override
    public void replaceReg(Map<VReg, MReg> replaceMap) {
        if (src instanceof VReg && replaceMap.containsKey(src))
            src = replaceMap.get(src);
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

    public Item getItem() {
        return item;
    }

    public int getImm() {
        return imm;
    }

    @Override
    public String toString() {
        return String.format("store\t%s,%d($%s)", src, imm, item.toString().toLowerCase());
    }
}
