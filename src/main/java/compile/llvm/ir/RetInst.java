package compile.llvm.ir;

import compile.llvm.type.BasicType;
import compile.llvm.value.Value;

public class RetInst extends Instruction {
    public final Value retVal;

    public RetInst(Value retVal) {
        super(BasicType.VOID);
        this.retVal = retVal;
    }

    @Override
    public String toString() {
        if (retVal == null)
            return "ret void";
        return String.format("ret %s %s", retVal.getType(), retVal.getName());
    }
}
