package compile.codegen.mirgen.mir;

import compile.codegen.Reg;
import compile.codegen.MReg;
import compile.codegen.VReg;
import compile.symbol.DataSymbol;

import java.util.List;
import java.util.Map;

public class LlaMIR extends MIR {
    public final Reg dest;
    public final DataSymbol symbol;

    public LlaMIR(Reg dest, DataSymbol symbol) {
        this.dest = dest;
        this.symbol = symbol;
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
        return new LlaMIR(newDest, symbol);
    }

    @Override
    public List<MIR> spill(Reg reg, int offset) {
        if (dest.equals(reg)) {
            VReg target = new VReg(reg.getType());
            MIR ir1 = new LlaMIR(target, symbol);
            MIR ir2 = new StoreItemMIR(StoreItemMIR.Item.SPILL, target, offset);
            return List.of(ir1, ir2);
        }
        return List.of(this);
    }

    @Override
    public String toString() {
        return String.format("lla\t%s,%s", dest, symbol.getName());
    }
}
