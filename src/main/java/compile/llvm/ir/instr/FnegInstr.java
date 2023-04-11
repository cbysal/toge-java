package compile.llvm.ir.instr;

import compile.llvm.ir.Value;
import compile.llvm.ir.type.BasicType;

public class FnegInstr extends Instr {
    private final Value base;

    public FnegInstr(Value base) {
        super(BasicType.FLOAT);
        this.base = base;
    }

    @Override
    public String toString() {
        return String.format("%s = fneg float %s", getTag(), base.getTag());
    }
}
