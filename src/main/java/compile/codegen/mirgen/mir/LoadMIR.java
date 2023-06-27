package compile.codegen.mirgen.mir;

import compile.codegen.Reg;
import compile.codegen.mirgen.MReg;
import compile.codegen.virgen.VReg;

import java.util.List;
import java.util.Map;

public class LoadMIR implements MIR {
    private Reg target, source;
    private final int imm, size;

    public LoadMIR(Reg target, Reg source, int imm, int size) {
        this.target = target;
        this.source = source;
        this.imm = imm;
        this.size = size;
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
            VReg target = new VReg(this.target.getType());
            VReg source = new VReg(this.source.getType());
            MIR ir1 = new LoadItemMIR(LoadItemMIR.Item.SPILL, source, offset);
            MIR ir2 = new LoadMIR(target, source, imm, size);
            MIR ir3 = new StoreItemMIR(StoreItemMIR.Item.SPILL, target, offset);
            return List.of(ir1, ir2, ir3);
        }
        if (target.equals(reg)) {
            VReg target = new VReg(this.target.getType());
            MIR ir1 = new LoadMIR(target, source, imm, size);
            MIR ir2 = new StoreItemMIR(StoreItemMIR.Item.SPILL, target, offset);
            return List.of(ir1, ir2);
        }
        if (source.equals(reg)) {
            VReg source = new VReg(this.source.getType());
            MIR ir1 = new LoadItemMIR(LoadItemMIR.Item.SPILL, source, offset);
            MIR ir2 = new LoadMIR(target, source, imm, size);
            return List.of(ir1, ir2);
        }
        return List.of(this);
    }

    @Override
    public String toString() {
        return String.format("%s\t%s,%d(%s)", switch (target.getType()) {
            case FLOAT -> "flw";
            case INT -> switch (size) {
                case 4 -> "lw";
                case 8 -> "ld";
                default -> throw new IllegalStateException("Unexpected value: " + size);
            };
            default -> throw new IllegalStateException("Unexpected value: " + target.getType());
        }, target, imm, source);
    }
}
