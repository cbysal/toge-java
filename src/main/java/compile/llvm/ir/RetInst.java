package compile.llvm.ir;

import compile.llvm.BasicBlock;
import compile.llvm.type.BasicType;
import compile.llvm.value.Value;

public class RetInst extends Instruction {
    public RetInst(BasicBlock block) {
        super(block, BasicType.VOID);
    }

    public RetInst(BasicBlock block, Value retVal) {
        super(block, BasicType.VOID, retVal);
    }

    @Override
    public String toString() {
        if (isEmpty())
            return "ret void";
        Value retVal = getOperand(0);
        return String.format("ret %s %s", retVal.getType(), retVal.getName());
    }
}
