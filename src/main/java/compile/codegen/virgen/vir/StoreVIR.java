package compile.codegen.virgen.vir;

import compile.codegen.virgen.VReg;
import compile.symbol.DataSymbol;

import java.util.ArrayList;
import java.util.List;

public class StoreVIR implements VIR {
    private final DataSymbol symbol;
    private final List<VIRItem> dimensions;
    private final VReg source;

    public StoreVIR(DataSymbol symbol, VReg source) {
        this(symbol, List.of(), source);
    }

    public StoreVIR(DataSymbol symbol, List<VIRItem> dimensions, VReg source) {
        this.symbol = symbol;
        this.dimensions = dimensions;
        this.source = source;
    }

    public List<VIRItem> getDimensions() {
        return dimensions;
    }

    @Override
    public List<VReg> getRead() {
        List<VReg> regs = new ArrayList<>();
        for (VIRItem dimension : dimensions)
            if (dimension instanceof VReg reg)
                regs.add(reg);
        regs.add(source);
        return regs;
    }

    public VReg getSource() {
        return source;
    }

    public DataSymbol getSymbol() {
        return symbol;
    }

    @Override
    public VReg getWrite() {
        return null;
    }

    public boolean isSingle() {
        return dimensions.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("STR     ").append(symbol.getName());
        dimensions.forEach(dimension -> builder.append('[').append(dimension).append(']'));
        builder.append(", ").append(source);
        return builder.toString();
    }
}
