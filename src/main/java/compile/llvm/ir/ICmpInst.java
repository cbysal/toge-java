package compile.llvm.ir;

import compile.llvm.value.Value;

public class ICmpInst extends CmpInst {

    public ICmpInst(Cond cond, Value op1, Value op2) {
        super(cond, op1, op2);
    }
}
