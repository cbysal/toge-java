package compile.llvm.ir;

import compile.llvm.BasicBlock;
import compile.llvm.value.Value;

public class ICmpInst extends CmpInst {
    public ICmpInst(BasicBlock block, Cond cond, Value op1, Value op2) {
        super(block, cond, op1, op2);
    }
}
