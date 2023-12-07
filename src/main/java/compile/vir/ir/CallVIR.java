package compile.vir.ir;

import compile.vir.VReg;
import compile.symbol.FuncSymbol;
import compile.vir.value.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CallVIR extends VIR {
    public final FuncSymbol func;
    public final List<Value> params;

    public CallVIR(FuncSymbol func, List<Value> params) {
        super(func.getType());
        this.func = func;
        this.params = params;
    }

    @Override
    public VIR copy() {
        return new CallVIR(func, new ArrayList<>(params));
    }

    @Override
    public List<VReg> getRead() {
        return params.stream().filter(VReg.class::isInstance).map(VReg.class::cast).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CALL    ").append(func.getName()).append(", ").append(getTag());
        params.forEach(param -> builder.append(", ").append(param instanceof VIR ir ? ir.getTag() : param));
        return builder.toString();
    }
}
