package compile.llvm.ir;

import compile.llvm.BasicBlock;
import compile.llvm.GlobalVariable;
import compile.llvm.type.PointerType;
import compile.llvm.type.Type;
import compile.llvm.value.Use;
import compile.llvm.value.Value;

public class GetElementPtrInst extends Instruction {
    public GetElementPtrInst(BasicBlock block, Value pointer, Value... indexes) {
        super(block, calcType(pointer, indexes.length), pointer);
        for (Value index : indexes)
            add(new Use(this, index));
    }

    private static Type calcType(Value value, int indexSize) {
        Type type = value.getType();
        if (value instanceof GlobalVariable)
            type = new PointerType(type);
        for (int i = 0; i < indexSize; i++)
            type = type.baseType();
        return new PointerType(type);
    }

    @Override
    public String toString() {
        Value pointer = getOperand(0);
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("%s = getelementptr %s, %s %s", getName(), switch (pointer) {
            case GlobalVariable global -> pointer.getType();
            default -> pointer.getType().baseType();
        }, switch (pointer) {
            case GlobalVariable global -> new PointerType(pointer.getType());
            default -> pointer.getType();
        }, pointer.getName()));
        for (int i = 1; i < size(); i++) {
            Value operand = getOperand(i);
            builder.append(", ").append(operand.getType()).append(" ").append(operand.getName());
        }
        return builder.toString();
    }
}
