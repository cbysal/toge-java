package compile.llvm.ir;

import common.ObjectUtils;
import compile.llvm.BasicBlock;
import compile.llvm.type.BasicType;
import compile.llvm.type.Type;
import compile.llvm.value.Use;
import compile.llvm.value.Value;

public abstract class CmpInst extends Instruction {
    private final Cond cond;

    protected CmpInst(BasicBlock block, Cond cond, Value operand1, Value operand2) {
        super(block, BasicType.I1);
        this.cond = cond;
        add(new Use(this, operand1));
        add(new Use(this, operand2));
    }

    public Cond getCond() {
        return cond;
    }

    @Override
    public String toString() {
        String operation = getClass().getSimpleName().toLowerCase();
        operation = operation.substring(0, operation.length() - 4);
        Cond cond = getCond();
        Value operand1 = getOperand(0);
        Value operand2 = getOperand(1);
        Type type = ObjectUtils.checkEquality(operand1.getType(), operand2.getType());
        return String.format("%s = %s %s %s %s, %s", getName(), operation, cond.toString().toLowerCase(), type, operand1.getName(), operand2.getName());
    }

    public enum Cond {
        EQ, NE, SGT, SGE, SLT, SLE, OEQ, OGT, OGE, OLT, OLE, UNE
    }
}
