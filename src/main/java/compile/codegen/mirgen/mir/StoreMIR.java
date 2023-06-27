package compile.codegen.mirgen.mir;

import compile.codegen.Reg;
import compile.codegen.mirgen.MReg;
import compile.codegen.virgen.VReg;

import java.util.List;
import java.util.Map;

public class StoreMIR implements MIR {
    private Reg source, target;
    private final int imm;
    private final int size;

    public StoreMIR(Reg source, Reg target, int imm, int size) {
        this.source = source;
        this.target = target;
        this.imm = imm;
        this.size = size;
    }

    @Override
    public List<Reg> getRegs() {
        return List.of(source, target);
    }

    @Override
    public List<Reg> getRead() {
        return List.of(source, target);
    }

    @Override
    public void replaceReg(Map<VReg, MReg> replaceMap) {
        if (source instanceof VReg && replaceMap.containsKey(source))
            source = replaceMap.get(source);
        if (target instanceof VReg && replaceMap.containsKey(target))
            target = replaceMap.get(target);
    }

    @Override
    public List<MIR> spill(Reg reg, int offset) {
        if (source.equals(reg) && target.equals(reg)) {
            VReg source = new VReg(this.source.getType());
            VReg target = new VReg(this.target.getType());
            MIR ir1 = new LoadItemMIR(LoadItemMIR.Item.SPILL, source, offset);
            MIR ir2 = new LoadItemMIR(LoadItemMIR.Item.SPILL, target, offset);
            MIR ir3 = new StoreMIR(source, target, imm, size);
            return List.of(ir1, ir2, ir3);
        }
        if (source.equals(reg)) {
            VReg source = new VReg(this.source.getType());
            MIR ir1 = new LoadItemMIR(LoadItemMIR.Item.SPILL, source, offset);
            MIR ir2 = new StoreMIR(source, target, imm, size);
            return List.of(ir1, ir2);
        }
        if (target.equals(reg)) {
            VReg target = new VReg(this.target.getType());
            MIR ir1 = new LoadItemMIR(LoadItemMIR.Item.SPILL, target, offset);
            MIR ir2 = new StoreMIR(source, target, imm, size);
            return List.of(ir1, ir2);
        }
        return List.of(this);
    }

    @Override
    public String toString() {
        return String.format("%s\t%s,%d(%s)", switch (source.getType()) {
            case FLOAT -> "fsw";
            case INT -> switch (size) {
                case 4 -> "sw";
                case 8 -> "sd";
                default -> throw new IllegalStateException("Unexpected value: " + size);
            };
            default -> throw new IllegalStateException("Unexpected value: " + source.getType());
        }, source, imm, target);
    }
}
