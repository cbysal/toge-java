package compile.codegen.virgen.vir;

import compile.codegen.virgen.VReg;
import compile.symbol.DataSymbol;

import java.util.ArrayList;
import java.util.List;

public class StoreVIR extends VIR {
    public final DataSymbol symbol;
    public final List<VIRItem> indexes;
    public final VReg source;

    public StoreVIR(DataSymbol symbol, List<VIRItem> indexes, VReg source) {
        this.symbol = symbol;
        this.indexes = indexes;
        this.source = source;
    }

    @Override
    public VIR copy() {
        return new StoreVIR(symbol, new ArrayList<>(indexes), source);
    }

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
