package compile.llvm.ir;

import compile.llvm.BasicBlock;
import compile.llvm.type.Type;
import compile.llvm.value.Use;
import compile.llvm.value.Value;

public abstract class CastInst extends Instruction {
    protected CastInst(BasicBlock block, Type type, Value operand) {
        super(block, type, operand);
        add(new Use(this, operand));
    }

    @Override
    public String toString() {
        String operation = getClass().getSimpleName().toLowerCase();
        operation = operation.substring(0, operation.length() - 4);
        Value operand = getOperand(0);
        return String.format("%s = %s %s %s to %s", getName(), operation, operand.getType(), operand.getName(), type);
    }
}
