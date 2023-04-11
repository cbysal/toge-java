package compile.llvm.ir.instr;

import compile.llvm.ir.Value;
import compile.llvm.ir.type.BasicType;

public class FcmpInstr extends Instr {
    public enum Type {
        OEQ, UNE, OGE, OGT, OLE, OLT
    }

    private final Type type;
    private final Value lVal, rVal;

    public FcmpInstr(Type type, Value lVal, Value rVal) {
        super(BasicType.I1);
        this.type = type;
        this.lVal = lVal;
        this.rVal = rVal;
    }

    @Override
    public String toString() {
        return String.format("%s = fcmp %s %s, %s", getTag(), type.name().toLowerCase(), lVal.getRet(), rVal.getTag());
    }
}
