package compile.codegen.virgen.vir;

import compile.codegen.virgen.VReg;
import compile.symbol.DataSymbol;

import java.util.ArrayList;
import java.util.List;

public class LoadVIR implements VIR {
    private final VReg target;
    private final List<VIRItem> dimensions;
    private final DataSymbol symbol;

    public LoadVIR(VReg target, DataSymbol symbol) {
        this(target, List.of(), symbol);
    }

    public LoadVIR(VReg target, List<VIRItem> dimensions, DataSymbol symbol) {
        this.target = target;
        this.dimensions = dimensions;
        this.symbol = symbol;
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
        return regs;
    }

    public DataSymbol getSymbol() {
        return symbol;
    }

    public VReg getTarget() {
        return target;
    }

    @Override
    public VReg getWrite() {
        return target;
    }

    public boolean isSingle() {
        return dimensions.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("LDR     ").append(target).append(", ");
        builder.append(symbol.getName());
        dimensions.forEach(dimension -> builder.append('[').append(dimension).append(']'));
        return builder.toString();
    }
}
