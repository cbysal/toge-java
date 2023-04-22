package compile.llvm.ir.instr;

import compile.llvm.ir.Value;
import compile.llvm.ir.type.Type;

public class ZextInstr extends Instr {
    private final Value value;

    public ZextInstr(Type type, Value value) {
        super(type);
        this.value = value;
    }

    public Value getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.format("%s = zext %s to %s", getTag(), value.getRet(), type);
    }
}
