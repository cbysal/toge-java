package compile.llvm.ir.instr;

import compile.llvm.ir.BasicBlock;
import compile.llvm.ir.Value;
import compile.llvm.ir.type.BasicType;

public class BranchInstr extends Instr {
    private final Value cond;
    private final BasicBlock trueBlock, falseBlock;

    public BranchInstr(BasicBlock block) {
        this(null, block, null);
    }

    public BranchInstr(Value cond, BasicBlock trueBlock, BasicBlock falseBlock) {
        super(BasicType.VOID);
        this.cond = cond;
        this.trueBlock = trueBlock;
        this.falseBlock = falseBlock;
        if (cond instanceof Instr instr) {
            instr.addUse(this);
        }
    }

    public Value getCond() {
        return cond;
    }

    public BasicBlock getTrueBlock() {
        return trueBlock;
    }

    public BasicBlock getFalseBlock() {
        return falseBlock;
    }

    @Override
    public String toString() {
        if (cond == null) {
            return String.format("br label %s", trueBlock.getTag());
        } else {
            return String.format("br %s, label %s, label %s", cond.getRet(), trueBlock.getTag(), falseBlock.getTag());
        }
    }
}
