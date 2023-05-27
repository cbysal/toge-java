package compile.llvm.ir.instr;

import compile.llvm.ir.Value;
import compile.llvm.ir.type.Type;

public class BitCastInstr extends Instr {
    private final Value base;

    public BitCastInstr(Value base, Type target) {
        super(target);
        this.base = base;
    }

    public Value getBase() {
        return base;
    }

    @Override
    public String toString() {
        return String.format("%s = bitcast %s to %s", getTag(), base.getRet(), type);
    }
}
