package compile.vir.ir;

import compile.symbol.GlobalSymbol;
import compile.vir.Argument;
import compile.vir.type.PointerType;
import compile.vir.value.Value;

public class LoadVIR extends VIR {
    public final Value pointer;

    public LoadVIR(Value pointer) {
        super(switch (pointer) {
            case GlobalSymbol global -> global.getType();
            case Argument arg -> arg.getType();
            default -> pointer.getType().getBaseType();
        });
        this.pointer = pointer;
    }

    @Override
    public String toString() {
        return String.format("%s = load %s, %s %s", getName(), type, switch (pointer) {
            case GlobalSymbol global -> new PointerType(global.getType());
            case Argument arg -> new PointerType(arg.getType());
            default -> pointer.getType();
        }, pointer.getName());
    }
}