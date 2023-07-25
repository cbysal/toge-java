package compile.codegen.virgen.vir;

import compile.codegen.virgen.VReg;
import compile.symbol.DataSymbol;

import java.util.List;
import java.util.stream.Collectors;

public record LoadVIR(VReg target, DataSymbol symbol, List<VIRItem> indexes) implements VIR {
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
