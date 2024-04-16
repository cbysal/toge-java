package compile.llvm.ir;

import compile.llvm.BasicBlock;
import compile.llvm.GlobalVariable;
import compile.llvm.type.PointerType;
import compile.llvm.value.Value;

public class LoadInst extends Instruction {
    public LoadInst(BasicBlock block, Value pointer) {
        super(block, pointer instanceof GlobalVariable ? pointer.getType() : pointer.getType().baseType(), pointer);
    }

    @Override
    public String toString() {
        Value pointer = getOperand(0);
        return String.format("%s = load %s, %s %s", getName(), type, pointer instanceof GlobalVariable ? new PointerType(pointer.getType()) : pointer.getType(), pointer.getName());
    }
}
