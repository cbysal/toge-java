package compile.llvm.ir.instr;

import compile.llvm.ir.Value;
import compile.llvm.ir.type.BasicType;

public class FcmpInstr extends Instr {
    public enum Op {
        OEQ, UNE, OGE, OGT, OLE, OLT
    }

    private final Op op;
    private final Value lVal, rVal;

    public FcmpInstr(Op op, Value lVal, Value rVal) {
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
        return String.format("%s = fcmp %s %s, %s", getTag(), op.name().toLowerCase(), lVal.getRet(), rVal.getTag());
    }
}
