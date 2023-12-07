package compile.vir.ir;

import compile.symbol.DataSymbol;
import compile.vir.type.BasicType;
import compile.vir.value.Value;

import java.util.List;

public class StoreVIR extends VIR {
    public final DataSymbol symbol;
    public final List<Value> indexes;
    public final Value source;

    public StoreVIR(DataSymbol symbol, List<Value> indexes, Value source) {
        super(BasicType.VOID);
        this.symbol = symbol;
        this.indexes = indexes;
        this.source = source;
    }

    public boolean isSingle() {
        return indexes.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("STORE   ").append(symbol.getName());
        indexes.forEach(dimension -> builder.append('[').append(dimension).append(']'));
        builder.append(", ").append(source instanceof VIR ir ? ir.getTag() : source);
        return builder.toString();
    }
}
