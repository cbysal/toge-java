package compile.llvm.ir.instr;

import compile.llvm.ir.Value;
import compile.llvm.ir.type.BasicType;

public class SitofpInstr extends Instr {
    private final Value base;

    public SitofpInstr(Value base) {
        super(BasicType.FLOAT);
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
        return String.format("%s = sitofp %s to float", getTag(), base.getRet());
    }
}
