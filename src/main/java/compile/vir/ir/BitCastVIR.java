package compile.vir.ir;

import compile.vir.type.Type;
import compile.vir.value.Value;

public class BitCastVIR extends VIR {
    private final Value value;

    public BitCastVIR(Type type, Value value) {
        super(type);
        this.value = value;
    }

    public Value getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.format("%s = bitcast %s %s to %s", getName(), value.getType(), value.getName(), type);
    }
}
