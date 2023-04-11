package compile.llvm.ir.instr;

import compile.llvm.ir.Value;
import compile.llvm.ir.type.BasicType;
import compile.llvm.ir.type.Type;

import java.util.List;

public class CallInstr extends Instr {
    private final String func;
    private final List<Value> params;

    public CallInstr(Type type, String func, List<Value> params) {
        super(type);
        this.func = func;
        this.params = params;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (type != BasicType.VOID) {
            builder.append(getTag()).append(" = ");
        }
        builder.append("call ").append(type).append('@').append(func).append('(');
        boolean isFirst = true;
        for (Value param : params) {
            if (!isFirst) {
                builder.append(", ");
            }
            builder.append(param.getRet());
            isFirst = false;
        }
        builder.append(')');
        return builder.toString();
    }
}
