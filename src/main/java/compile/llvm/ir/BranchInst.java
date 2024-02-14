package compile.llvm.ir;

import compile.llvm.BasicBlock;
import compile.llvm.type.BasicType;
import compile.llvm.value.Value;

public class BranchInst extends Instruction {
    public BranchInst(BasicBlock dest) {
        super(BasicType.VOID, dest);
    }

    public BranchInst(Value cond, BasicBlock ifTrue, BasicBlock ifFalse) {
        super(BasicType.VOID, cond, ifTrue, ifFalse);
    }

    public boolean conditional() {
        return size() == 3;
    }

    @Override
    public String toString() {
        if (conditional()) {
            Value cond = getOperand(0);
            BasicBlock ifTrue = getOperand(1);
            BasicBlock ifFalse = getOperand(2);
            return String.format("br i1 %s, label %%%s, label %%%s", cond.getName(), ifTrue, ifFalse);
        }
        BasicBlock dest = getOperand(0);
        return String.format("br label %%%s", dest);
    }
}
