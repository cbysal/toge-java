package compile.codegen.mirgen.mir;

import compile.codegen.Reg;
import compile.codegen.mirgen.MReg;
import compile.codegen.virgen.VReg;
import compile.symbol.Symbol;
import compile.symbol.Type;

import java.util.List;
import java.util.Map;

public class LlaMIR implements MIR {
    private Reg target;
    private final Symbol symbol;

    public LlaMIR(Reg target, Symbol symbol) {
        this.target = target;
        this.symbol = symbol;
    }

    @Override
    public List<Reg> getRegs() {
        return List.of(target);
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
            MIR ir1 = new LlaMIR(target, symbol);
            MIR ir2 = new StoreItemMIR(StoreItemMIR.Item.SPILL, target, offset);
            return List.of(ir1, ir2);
        }
        return List.of(this);
    }

    @Override
    public String toString() {
        return String.format("lla\t%s,%s", target, symbol.getName());
    }
}
