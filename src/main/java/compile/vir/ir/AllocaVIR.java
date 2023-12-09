package compile.vir.ir;

import compile.vir.type.Type;

public class AllocaVIR extends VIR {
    public AllocaVIR(Type type) {
        super(type);
    }

    @Override
    public String toString() {
        return String.format("%s = alloca %s", getTag(), type);
    }
}
