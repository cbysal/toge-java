package compile.vir.ir;

import compile.symbol.DataSymbol;
import compile.vir.type.ArrayType;
import compile.vir.type.PointerType;
import compile.vir.type.Type;
import compile.vir.value.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GetElementPtrVIR extends VIR {
    private final Value pointer;
    private final List<Value> indexes;

    private static Type calcType(Value value, int indexSize) {
        Type type = value.getType();
        if (value instanceof DataSymbol symbol) {
            for (int i = symbol.getDimensionSize() - 1; i >= 0; i--)
                type = new ArrayType(type, symbol.getDimensions().get(i));
            type = new PointerType(type);
        }
        for (int i = 0; i < indexSize; i++) {
            type = switch (type) {
                case PointerType pointerType -> pointerType.getBaseType();
                case ArrayType arrayType -> arrayType.getBaseType();
                default -> throw new IllegalStateException("Unexpected value: " + type);
            };
        }
        return new PointerType(type);
    }

    public GetElementPtrVIR(Value pointer, Value... indexes) {
        super(calcType(pointer, indexes.length));
        this.pointer = pointer;
        this.indexes = new ArrayList<>(Arrays.asList(indexes));
    }

    public Value getPointer() {
        return pointer;
    }

    public List<Value> getIndexes() {
        return indexes;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getName()).append(" = getelementptr ").append(getType()).append(", ").append(pointer.getType()).append(" ").append(pointer.getName());
        for (Value index : indexes)
            builder.append(", ").append(index.getType()).append(" ").append(index.getName());
        return builder.toString();
    }
}
