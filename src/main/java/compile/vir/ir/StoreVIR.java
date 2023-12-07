package compile.vir.ir;

import compile.vir.VReg;
import compile.symbol.DataSymbol;
import compile.vir.type.BasicType;
import compile.vir.value.Value;

import java.util.ArrayList;
import java.util.List;

public class StoreVIR extends VIR {
    public final DataSymbol symbol;
    public final List<Value> indexes;
    public final VReg source;

    public StoreVIR(DataSymbol symbol, List<Value> indexes, VReg source) {
        super(BasicType.VOID);
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
        for (Value item : indexes)
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
