package compile.llvm.ir.instr;

import compile.llvm.ir.Value;
import compile.llvm.ir.type.BasicType;

public class FptosiInstr extends Instr {
    private final Value base;

    public FptosiInstr(Value base) {
        super(BasicType.I32);
        this.base = base;
        if (base instanceof Instr instr) {
            instr.addUse(this);
        }
    }

    public Value getBase() {
        return base;
    }

    @Override
    public String toString() {
        return String.format("%s = fptosi %s to i32", getTag(), base.getRet());
    }
}
