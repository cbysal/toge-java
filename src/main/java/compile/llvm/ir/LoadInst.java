package compile.llvm.ir;

import compile.llvm.BasicBlock;
import compile.llvm.GlobalVariable;
import compile.llvm.type.PointerType;
import compile.llvm.value.Value;

public class LoadInst extends Instruction {
    public LoadInst(BasicBlock block, Value pointer) {
        super(block, switch (pointer) {
            case GlobalVariable global -> pointer.getType();
            default -> pointer.getType().baseType();
        }, pointer);
    }

    @Override
    public String toString() {
        Value pointer = getOperand(0);
        return String.format("%s = load %s, %s %s", getName(), type, switch (pointer) {
            case GlobalVariable global -> new PointerType(pointer.getType());
            default -> pointer.getType();
        }, pointer.getName());
    }
}
