package compile.llvm.ir.instr;

import compile.llvm.ir.Value;
import compile.llvm.ir.type.BasicType;

public class CmpInstr extends Instr {
    public enum Op {
        EQ, NE, SGE, SGT, SLE, SLT
    }

    private final Op op;
    private final Value lVal, rVal;

    public CmpInstr(Op op, Value lVal, Value rVal) {
        super(BasicType.I1);
        this.op = op;
        this.lVal = lVal;
        this.rVal = rVal;
        if (lVal instanceof Instr instr) {
            instr.addUse(this);
        }
        if (rVal instanceof Instr instr) {
            instr.addUse(this);
        }
    }

    public Op getOp() {
        return op;
    }

    public Value getlVal() {
        return lVal;
    }

    public Value getrVal() {
        return rVal;
    }

    @Override
    public String toString() {
        return String.format("%s = icmp %s %s, %s", getTag(), op.name().toLowerCase(), lVal.getRet(), rVal.getTag());
    }
}
