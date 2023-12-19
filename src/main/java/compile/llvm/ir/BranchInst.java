package compile.llvm.ir;

import compile.llvm.BasicBlock;
import compile.llvm.type.BasicType;
import compile.llvm.value.Value;

public class BranchInst extends Instruction {
    private final Value cond;
    private final BasicBlock ifTrue, ifFalse;
    private final BasicBlock dest;

    public BranchInst(Value cond, BasicBlock ifTrue, BasicBlock ifFalse) {
        super(BasicType.VOID);
        this.cond = cond;
        this.ifTrue = ifTrue;
        this.ifFalse = ifFalse;
        this.dest = null;
    }

    public BranchInst(BasicBlock dest) {
        super(BasicType.VOID);
        this.cond = null;
        this.ifTrue = null;
        this.ifFalse = null;
        this.dest = dest;
    }

    public Value getCond() {
        return cond;
    }

    public BasicBlock getIfTrue() {
        return ifTrue;
    }

    public BasicBlock getIfFalse() {
        return ifFalse;
    }

    public BasicBlock getDest() {
        return dest;
    }

    public boolean conditional() {
        return cond != null;
    }

    @Override
    public String toString() {
        if (conditional())
            return String.format("br i1 %s, label %%%s, label %%%s", cond.getName(), ifTrue, ifFalse);
        return String.format("br label %%%s", dest);
    }
}
