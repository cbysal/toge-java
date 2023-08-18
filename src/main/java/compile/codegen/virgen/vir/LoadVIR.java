package compile.codegen.virgen.vir;

import compile.codegen.virgen.VReg;
import compile.symbol.DataSymbol;

import java.util.List;
import java.util.stream.Collectors;

public class LoadVIR extends VIR {
    private final VReg target;
    private final DataSymbol symbol;
    private final List<VIRItem> indexes;

    public LoadVIR(VReg target, DataSymbol symbol, List<VIRItem> indexes) {
        this.target = target;
        this.symbol = symbol;
        this.indexes = indexes;
    }

    public VReg target() {
        return target;
    }

    public DataSymbol symbol() {
        return symbol;
    }

    public List<VIRItem> indexes() {
        return indexes;
    }

    @Override
    public List<VReg> getRead() {
        return indexes.stream().filter(VReg.class::isInstance).map(VReg.class::cast).collect(Collectors.toList());
    }

    @Override
    public VReg getWrite() {
        return target;
    }

    public boolean isSingle() {
        return indexes.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("LOAD    ").append(target).append(", ");
        builder.append(symbol.getName());
        indexes.forEach(dimension -> builder.append('[').append(dimension).append(']'));
        return builder.toString();
    }
}
