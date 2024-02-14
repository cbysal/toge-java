package compile.llvm.ir;

import compile.llvm.GlobalVariable;
import compile.llvm.type.BasicType;
import compile.llvm.type.PointerType;
import compile.llvm.value.Value;

public class StoreInst extends Instruction {
    public StoreInst(Value value, Value pointer) {
        super(BasicType.VOID, value, pointer);
    }

    @Override
    public String toString() {
        Value value = getOperand(0);
        Value pointer = getOperand(1);
        return String.format("store %s %s, %s %s", value.getType(), value.getName(), switch (pointer) {
            case GlobalVariable global -> new PointerType(global.getType());
            default -> pointer.getType();
        }, pointer.getName());
    }
}
