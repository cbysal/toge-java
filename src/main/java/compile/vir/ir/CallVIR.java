package compile.vir.ir;

import compile.vir.VirtualFunction;
import compile.vir.type.BasicType;
import compile.vir.value.Value;

import java.util.List;
import java.util.StringJoiner;

public class CallVIR extends VIR {
    public final VirtualFunction func;
    public final List<Value> params;

    public CallVIR(VirtualFunction func, List<Value> params) {
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
