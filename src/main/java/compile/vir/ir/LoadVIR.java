package compile.vir.ir;

import compile.symbol.DataSymbol;
import compile.symbol.Symbol;
import compile.vir.type.ArrayType;
import compile.vir.type.BasicType;
import compile.vir.type.Type;
import compile.vir.value.Value;

import java.util.List;

public class LoadVIR extends VIR {
    public final Value symbol;
    public final List<Value> indexes;

    private static Type calcType(Value value, List<Value> indexes) {
        if (value instanceof AllocaVIR allocaVIR) {
            Type type = allocaVIR.getType();
            int indexSize = indexes.size();
            while (type instanceof ArrayType arrayType) {
                type = arrayType.getBaseType();
                indexSize--;
            }
            if (indexSize != 0)
                return BasicType.I32;
            return type;
        }
        if (value instanceof DataSymbol symbol)
            return symbol.getDimensionSize() != indexes.size() ? BasicType.I32 : symbol.getType();
        throw new RuntimeException("Unexpected value: " + value);
    }

    public LoadVIR(Value symbol, List<Value> indexes) {
        super(calcType(symbol, indexes));
        this.symbol = symbol;
        this.indexes = indexes;
    }

    public boolean isSingle() {
        return indexes.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("LOAD    ").append(getTag()).append(", ");
        builder.append(switch (symbol) {
            case Symbol sym -> sym.getName();
            case VIR ir -> ir.getTag();
            default -> throw new IllegalStateException("Unexpected value: " + symbol);
        });
        indexes.forEach(dimension -> builder.append('[').append(dimension instanceof VIR ir ? ir.getTag() : dimension).append(']'));
        return builder.toString();
    }
}
