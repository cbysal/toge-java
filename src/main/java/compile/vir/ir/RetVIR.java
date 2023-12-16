package compile.vir.ir;

import compile.vir.type.BasicType;
import compile.vir.value.Value;

public class RetVIR extends VIR {
    public final Value retVal;

    public RetVIR(Value retVal) {
        super(BasicType.VOID);
        this.retVal = retVal;
    }

    @Override
    public String toString() {
        if (retVal == null)
            return "ret void";
        return String.format("ret %s %s", retVal.getType(), retVal.getName());
    }
}
