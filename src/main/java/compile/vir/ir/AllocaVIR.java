package compile.vir.ir;

import compile.vir.type.PointerType;
import compile.vir.type.Type;

public class AllocaVIR extends VIR {
    public AllocaVIR(Type type) {
        super(new PointerType(type));
    }

    @Override
    public String toString() {
        return String.format("%s = alloca %s", getName(), type.baseType());
    }
}
