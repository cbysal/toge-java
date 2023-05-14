package compile.llvm.ir.instr;

import compile.llvm.ir.Value;
import compile.llvm.ir.type.BasicType;

public class RetInstr extends Instr {
    private final Value retValue;

    public RetInstr() {
        this(null);
    }

    public RetInstr(Value retValue) {
        super(BasicType.VOID);
        this.retValue = retValue;
        if (retValue instanceof Instr instr) {
            instr.addUse(this);
        }
    }

    public Value getRetValue() {
        return retValue;
    }

    @Override
    public String toString() {
        if (retValue == null) {
            return "ret void";
        }
        return String.format("ret %s", retValue.getRet());
    }
}
