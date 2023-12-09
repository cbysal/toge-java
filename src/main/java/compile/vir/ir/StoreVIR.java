package compile.vir.ir;

import compile.symbol.Symbol;
import compile.vir.type.BasicType;
import compile.vir.value.Value;

import java.util.List;

public class StoreVIR extends VIR {
    public final Value symbol;
    public final List<Value> indexes;
    public final Value source;

    public StoreVIR(Value symbol, List<Value> indexes, Value source) {
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
        builder.append("STORE   ").append(switch (symbol) {
            case Symbol sym -> sym.getName();
            case VIR ir -> ir.getTag();
            default -> throw new IllegalStateException("Unexpected value: " + symbol);
        });
        indexes.forEach(dimension -> builder.append('[').append(dimension).append(']'));
        builder.append(", ").append(source instanceof VIR ir ? ir.getTag() : source);
        return builder.toString();
    }
}
