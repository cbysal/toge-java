package compile.vir.ir;

import compile.vir.type.Type;
import compile.vir.value.Value;

public class FPToSIVIR extends VIR {
    private final Value value;

    public FPToSIVIR(Type type, Value value) {
        super(type);
        this.value = value;
    }

    public Value getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.format("%s = fptosi %s %s to %s", getName(), value.getType(), value.getName(), type);
    }
}
