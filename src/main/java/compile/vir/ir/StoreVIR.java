package compile.vir.ir;

import compile.vir.GlobalVariable;
import compile.vir.type.BasicType;
import compile.vir.type.PointerType;
import compile.vir.value.Value;

public class StoreVIR extends VIR {
    public final Value value;
    public final Value pointer;

    public StoreVIR(Value value, Value pointer) {
        super(BasicType.VOID);
        this.value = value;
        this.pointer = pointer;
    }

    @Override
    public String toString() {
        return String.format("store %s %s, %s %s", value.getType(), value.getName(), switch (pointer) {
            case GlobalVariable global -> new PointerType(global.getType());
            default -> pointer.getType();
        }, pointer.getName());
    }
}
