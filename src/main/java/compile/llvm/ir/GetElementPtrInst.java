package compile.llvm.ir;

import compile.llvm.GlobalVariable;
import compile.llvm.type.PointerType;
import compile.llvm.type.Type;
import compile.llvm.value.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GetElementPtrInst extends Instruction {
    private final Value pointer;
    private final List<Value> indexes;

    public GetElementPtrInst(Value pointer, Value... indexes) {
        super(calcType(pointer, indexes.length));
        this.pointer = pointer;
        this.indexes = new ArrayList<>(Arrays.asList(indexes));
    }

    private static Type calcType(Value value, int indexSize) {
        Type type = value.getType();
        if (value instanceof GlobalVariable)
            type = new PointerType(type);
        for (int i = 0; i < indexSize; i++)
            type = type.baseType();
        return new PointerType(type);
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
        builder.append(String.format("%s = getelementptr %s, %s %s", getName(), switch (pointer) {
            case GlobalVariable global -> pointer.getType();
            default -> pointer.getType().baseType();
        }, switch (pointer) {
            case GlobalVariable global -> new PointerType(pointer.getType());
            default -> pointer.getType();
        }, pointer.getName()));
        for (Value index : indexes)
            builder.append(", ").append(index.getType()).append(" ").append(index.getName());
        return builder.toString();
    }
}
