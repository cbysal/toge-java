package compile.llvm.ir;

import compile.llvm.value.Value;

public class FCmpInst extends CmpInst {
    public FCmpInst(Cond cond, Value operand1, Value operand2) {
        super(cond, operand1, operand2);
    }
}
