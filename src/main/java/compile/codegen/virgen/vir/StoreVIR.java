package compile.codegen.virgen.vir;

import compile.codegen.virgen.VReg;
import compile.symbol.DataSymbol;

import java.util.ArrayList;
import java.util.List;

public record StoreVIR(DataSymbol symbol, List<VIRItem> indexes, VReg source) implements VIR {
    @Override
    public List<VReg> getRead() {
        List<VReg> regs = new ArrayList<>();
        for (VIRItem item : indexes)
            if (item instanceof VReg reg)
                regs.add(reg);
        regs.add(source);
        return regs;
    }

    public boolean isSingle() {
        return indexes.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("STORE   ").append(symbol.getName());
        indexes.forEach(dimension -> builder.append('[').append(dimension).append(']'));
        builder.append(", ").append(source);
        return builder.toString();
    }
}
