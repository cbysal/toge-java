package compile.codegen.mirgen.mir;

import compile.codegen.Reg;
import compile.codegen.mirgen.MReg;
import compile.codegen.virgen.VReg;
import compile.symbol.Type;

import java.util.List;
import java.util.Map;

public class LoadItemMIR implements MIR {
    public enum Item {
        SPILL, PARAM_INNER, PARAM_OUTER, LOCAL
    }

    private final Item item;
    private Reg dest;
    private final int imm;

    public LoadItemMIR(Item item, Reg dest, int imm) {
        this.item = item;
        this.dest = dest;
        this.imm = imm;
    }

    @Override
    public List<Reg> getRegs() {
        return List.of(dest);
    }

    public Reg getDest() {
        return dest;
    }

    @Override
    public List<Reg> getWrite() {
        return List.of(dest);
    }

    @Override
    public void replaceReg(Map<VReg, MReg> replaceMap) {
        if (dest instanceof VReg && replaceMap.containsKey(dest))
            dest = replaceMap.get(dest);
    }

    @Override
    public List<MIR> spill(Reg reg, int offset) {
        if (dest.equals(reg)) {
            VReg target = new VReg(Type.INT);
            MIR ir1 = new LoadItemMIR(item, target, imm);
            MIR ir2 = new StoreItemMIR(StoreItemMIR.Item.SPILL, target, offset);
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
        return String.format("load\t%s,%d($%s)", dest, imm, item);
    }
}
