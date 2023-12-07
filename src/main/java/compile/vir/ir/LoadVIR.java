package compile.vir.ir;

import compile.vir.VReg;
import compile.symbol.DataSymbol;
import compile.vir.value.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LoadVIR extends VIR {
    public final VReg target;
    public final DataSymbol symbol;
    public final List<Value> indexes;

    public LoadVIR(VReg target, DataSymbol symbol, List<Value> indexes) {
        super(target.getType());
        this.target = target;
        this.symbol = symbol;
        this.indexes = indexes;
    }

    @Override
    public VIR copy() {
        return new LoadVIR(target, symbol, new ArrayList<>(indexes));
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
