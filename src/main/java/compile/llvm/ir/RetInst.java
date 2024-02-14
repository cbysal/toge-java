package compile.llvm.ir;

import compile.llvm.type.BasicType;
import compile.llvm.value.Value;

public class RetInst extends Instruction {
    public RetInst() {
        super(BasicType.VOID);
    }

    public RetInst(Value retVal) {
        super(BasicType.VOID, retVal);
    }

    @Override
    public String toString() {
        if (isEmpty())
            return "ret void";
        Value retVal = getOperand(0);
        return String.format("ret %s %s", retVal.getType(), retVal.getName());
    }
}
