package compile.llvm.ir.instr;

import compile.llvm.ir.Value;

import java.util.Map;

public class FCmpInstr extends CmpInstr {
    private static final Map<Op, String> OP_STRING_MAP = Map.of(Op.EQ, "oeq", Op.NE, "une", Op.GE, "oge", Op.GT, "ogt"
            , Op.LE, "ole", Op.LT, "olt");

    public FCmpInstr(Op op, Value lVal, Value rVal) {
        super(op, lVal, rVal);
    }

    @Override
    public String toString() {
        return String.format("%s = fcmp %s %s, %s", getTag(), OP_STRING_MAP.get(op), lVal.getRet(), rVal.getTag());
    }
}
