package compile.llvm.ir;

import compile.llvm.BasicBlock;
import compile.llvm.GlobalVariable;
import compile.llvm.type.BasicType;
import compile.llvm.type.PointerType;
import compile.llvm.type.Type;
import compile.llvm.value.Value;

import java.util.Objects;

public class StoreInst extends Instruction {
    public StoreInst(BasicBlock block, Value value, Value pointer) {
        super(block, BasicType.VOID, value, pointer);
    }

    @Override
    public String toString() {
        Value value = getOperand(0);
        Value pointer = getOperand(1);
        Type type1;
        if (Objects.requireNonNull(pointer) instanceof GlobalVariable) {
            GlobalVariable global = (GlobalVariable) pointer;
            type1 = new PointerType(global.getType());
        } else {
            type1 = pointer.getType();
        }
        return String.format("store %s %s, %s %s", value.getType(), value.getName(), type1, pointer.getName());
    }
}
