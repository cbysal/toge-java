package compile.llvm.ir;

import compile.llvm.type.Type;
import compile.llvm.value.Value;

public class SIToFPInst extends Instruction {
    private final Value value;

    public SIToFPInst(Type type, Value value) {
        super(type);
        this.value = value;
    }

    public Value getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.format("%s = sitofp %s %s to %s", getName(), value.getType(), value.getName(), type);
    }
}
