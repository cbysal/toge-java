package compile.vir.ir;

import compile.symbol.FuncSymbol;
import compile.vir.value.Value;

import java.util.List;

public class CallVIR extends VIR {
    public final FuncSymbol func;
    public final List<Value> params;

    public CallVIR(FuncSymbol func, List<Value> params) {
        super(func.getType());
        this.func = func;
        this.params = params;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CALL    ").append(func.getName()).append(", ").append(getName());
        params.forEach(param -> builder.append(", ").append(param.getName()));
        return builder.toString();
    }
}
