package compile.llvm.ir.instr;

import compile.llvm.ir.Value;

import java.util.Map;

public class ICmpInstr extends CmpInstr {
    private static final Map<Op, String> OP_STRING_MAP = Map.of(Op.EQ, "eq", Op.NE, "ne", Op.GE, "sge", Op.GT, "sgt",
            Op.LE, "sle", Op.LT, "slt");

    public ICmpInstr(Op op, Value lVal, Value rVal) {
        super(op, lVal, rVal);
    }

    @Override
    public String toString() {
        return String.format("%s = icmp %s %s, %s", getTag(), OP_STRING_MAP.get(op), lVal.getRet(), rVal.getTag());
    }
}
