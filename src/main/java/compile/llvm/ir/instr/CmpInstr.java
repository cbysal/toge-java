package compile.llvm.ir.instr;

import compile.llvm.ir.Value;
import compile.llvm.ir.type.BasicType;

public class CmpInstr extends Instr {
    public enum Type {
        EQ, NE, SGE, SGT, SLE, SLT
    }

    private final Type type;
    private final Value lVal, rVal;

    public CmpInstr(Type type, Value lVal, Value rVal) {
        super(BasicType.I1);
        this.type = type;
        this.lVal = lVal;
        this.rVal = rVal;
    }

    @Override
    public String toString() {
        return String.format("%s = icmp %s %s, %s", getTag(), type.name().toLowerCase(), lVal.getRet(), rVal.getTag());
    }
}
