package compile.llvm.ir;

import compile.llvm.BasicBlock;
import compile.llvm.type.BasicType;
import compile.llvm.value.Value;

public class BranchInst extends Instruction {
    public BranchInst(BasicBlock block, BasicBlock dest) {
        super(block, BasicType.VOID, dest);
    }

    public BranchInst(BasicBlock block, Value cond, BasicBlock ifTrue, BasicBlock ifFalse) {
        super(block, BasicType.VOID, cond, ifTrue, ifFalse);
    }

    public boolean isConditional() {
        return size() == 3;
    }

    @Override
    public String toString() {
        if (isConditional()) {
            Value cond = getOperand(0);
            BasicBlock ifTrue = getOperand(1);
            BasicBlock ifFalse = getOperand(2);
            return String.format("br i1 %s, label %%%s, label %%%s", cond.getName(), ifTrue, ifFalse);
        }
        BasicBlock dest = getOperand(0);
        return String.format("br label %%%s", dest);
    }
}
