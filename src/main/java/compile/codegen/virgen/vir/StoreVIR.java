package compile.codegen.virgen.vir;

import compile.codegen.virgen.VReg;
import compile.symbol.DataSymbol;

import java.util.List;
import java.util.stream.Collectors;

public record StoreVIR(DataSymbol symbol, List<VIRItem> indexes, VReg source) implements VIR {
    @Override
    public List<VReg> getRead() {
        return indexes.stream().filter(VReg.class::isInstance).map(VReg.class::cast).collect(Collectors.toList());
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
