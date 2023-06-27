package compile.codegen.virgen.vir;

import compile.codegen.virgen.VReg;
import compile.symbol.FuncSymbol;

import java.util.ArrayList;
import java.util.List;

public class CallVIR implements VIR {
    private final FuncSymbol func;
    private final VReg retVal;
    private final List<VIRItem> params;

    public CallVIR(FuncSymbol func, VReg retVal, List<VIRItem> params) {
        this.func = func;
        this.retVal = retVal;
        this.params = params;
    }

    public FuncSymbol getFunc() {
        return func;
    }

    public List<VIRItem> getParams() {
        return params;
    }

    @Override
    public List<VReg> getRead() {
        List<VReg> regs = new ArrayList<>();
        for (VIRItem param : params)
            if (param instanceof VReg reg)
                regs.add(reg);
        return regs;
    }

    public VReg getRetVal() {
        return retVal;
    }

    @Override
    public VReg getWrite() {
        return retVal;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CALL    ").append(func.getName()).append(", ").append(retVal == null ? "$void" : retVal);
        params.forEach(param -> builder.append(", ").append(param));
        return builder.toString();
    }
}
