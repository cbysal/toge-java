package compile.llvm.ir;

import compile.llvm.Function;
import compile.llvm.type.BasicType;
import compile.llvm.value.Use;
import compile.llvm.value.Value;

import java.util.List;
import java.util.StringJoiner;

public class CallInst extends Instruction {
    public CallInst(Function func, List<Value> params) {
        super(func.getType(), func);
        for (Value param : params)
            add(new Use(this, param));
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", "(", ")");
        for (int i = 1; i < operands.size(); i++) {
            Value param = getOperand(i);
            joiner.add(String.format("%s %s", param.getType(), param.getName()));
        }
        Function func = getOperand(0);
        if (func.getType() == BasicType.VOID)
            return String.format("call %s %s%s", func.getType(), func.getName(), joiner);
        return String.format("%s = call %s %s%s", getName(), func.getType(), func.getName(), joiner);
    }
}
