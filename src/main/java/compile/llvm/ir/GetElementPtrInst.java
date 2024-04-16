package compile.llvm.ir;

import compile.llvm.BasicBlock;
import compile.llvm.GlobalVariable;
import compile.llvm.type.PointerType;
import compile.llvm.type.Type;
import compile.llvm.value.Use;
import compile.llvm.value.Value;

import java.util.Objects;

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
        Type type1;
        if (Objects.requireNonNull(pointer) instanceof GlobalVariable) {
            type1 = pointer.getType();
        } else {
            type1 = pointer.getType().baseType();
        }
        Type type2;
        if (pointer instanceof GlobalVariable) {
            type2 = new PointerType(pointer.getType());
        } else {
            type2 = pointer.getType();
        }
        builder.append(String.format("%s = getelementptr %s, %s %s", getName(), type1, type2, pointer.getName()));
        for (int i = 1; i < size(); i++) {
            Value operand = getOperand(i);
            builder.append(", ").append(operand.getType()).append(" ").append(operand.getName());
        }
        return builder.toString();
    }
}
