package compile.llvm.ir;

import compile.llvm.Function;
import compile.llvm.type.BasicType;
import compile.llvm.value.Value;

import java.util.List;
import java.util.StringJoiner;

public class CallInst extends Instruction {
    public final Function func;
    public final List<Value> params;

    public CallInst(Function func, List<Value> params) {
        super(func.getType());
        this.func = func;
        this.params = params;
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", "(", ")");
        params.forEach(param -> joiner.add(String.format("%s %s", param.getType(), param.getName())));
        if (func.getType() == BasicType.VOID)
            return String.format("call %s %s%s", func.getType(), func.getName(), joiner);
        return String.format("%s = call %s %s%s", getName(), func.getType(), func.getName(), joiner);
    }
}
