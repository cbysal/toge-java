package compile.llvm.ir;

import compile.llvm.BasicBlock;
import compile.llvm.type.PointerType;
import compile.llvm.type.Type;

public class AllocaInst extends Instruction {
    public AllocaInst(BasicBlock block, Type type) {
        super(block, new PointerType(type));
    }

    @Override
    public String toString() {
        return String.format("%s = alloca %s", getName(), type.baseType());
    }
}
