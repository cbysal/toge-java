package compile.llvm.ir;

import compile.llvm.BasicBlock;
import compile.llvm.value.Value;

public class FCmpInst extends CmpInst {
    public FCmpInst(BasicBlock block, Cond cond, Value operand1, Value operand2) {
        super(block, cond, operand1, operand2);
    }
}
