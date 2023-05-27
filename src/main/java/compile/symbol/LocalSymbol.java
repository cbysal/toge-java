package compile.symbol;

import compile.llvm.ir.type.Type;

public class LocalSymbol extends DataSymbol {
    LocalSymbol(Type type, String name) {
        super(type, name);
    }

    @Override
    public String toString() {
        return String.format("%s %s", type, name);
    }
}
