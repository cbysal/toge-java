package compile.llvm.ir;

import common.ObjectUtils;
import compile.llvm.type.Type;
import compile.llvm.value.Value;

public class BinaryOperator extends Instruction {
    public final Op op;

    public BinaryOperator(Op op, Value operand1, Value operand2) {
        super(ObjectUtils.checkEquality(operand1.getType(), operand2.getType()), operand1, operand2);
        this.op = op;
    }

    @Override
    public String toString() {
        Value operand1 = getOperand(0);
        Value operand2 = getOperand(1);
        Type type = ObjectUtils.checkEquality(operand1.getType(), operand2.getType());
        return String.format("%s = %s %s %s, %s", getName(), op.toString().toLowerCase(), type, operand1.getName(), operand2.getName());
    }

    public enum Op {
        ADD, FADD, SUB, FSUB, MUL, FMUL, SDIV, FDIV, SREM, XOR
    }
}
