package compile.llvm.ir.instr;

import compile.llvm.ir.Value;

public class BinaryInstr extends Instr {
    public enum Op {
        ADD, SUB, MUL, SDIV, SREM, XOR, FADD, FSUB, FMUL, FDIV
    }

    private final Op op;
    private final Value lVal, rVal;

    public BinaryInstr(Op op, Value lVal, Value rVal) {
        super(lVal.getType());
        this.op = op;
        this.lVal = lVal;
        this.rVal = rVal;
    }

    public Op getOp() {
        return op;
    }

    public Value getlVal() {
        return lVal;
    }

    public Value getrVal() {
        return rVal;
    }

    @Override
    public String toString() {
        return String.format("%s = %s %s %s, %s", getTag(), op.name().toLowerCase(), super.type, lVal.getTag(),
                rVal.getTag());
    }
}
