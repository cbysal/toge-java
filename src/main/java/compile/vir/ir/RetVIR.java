package compile.vir.ir;

import compile.vir.VReg;
import compile.vir.type.BasicType;
import compile.vir.value.Value;

import java.util.List;

public class RetVIR extends VIR {
    public final Value retVal;

    public RetVIR(Value retVal) {
        super(BasicType.VOID);
        this.retVal = retVal;
    }

    @Override
    public VIR copy() {
        return new RetVIR(retVal);
    }

    @Override
    public List<VReg> getRead() {
        if (retVal instanceof VReg reg)
            return List.of(reg);
        return List.of();
    }

    @Override
    public String toString() {
        if (retVal == null)
            return "RET";
        return "RET     " + retVal;
    }
}
