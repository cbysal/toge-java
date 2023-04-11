package compile.llvm.ir.instr;

import compile.llvm.ir.Value;

public class BinaryInstr extends Instr {
    public enum Type {
        ADD, SUB, MUL, SDIV, SREM, XOR, FADD, FSUB, FMUL, FDIV
    }

    private final Type type;
    private final Value lVal, rVal;

    public BinaryInstr(Type type, Value lVal, Value rVal) {
        super(lVal.getType());
        this.type = type;
        this.lVal = lVal;
        this.rVal = rVal;
    }

    @Override
    public String toString() {
        return String.format("%s = %s %s %s, %s", getTag(), type.name().toLowerCase(), super.type, lVal.getTag(),
                rVal.getTag());
    }
}
