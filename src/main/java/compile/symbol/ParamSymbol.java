package compile.symbol;

import compile.llvm.ir.type.Type;

public class ParamSymbol extends DataSymbol {
    ParamSymbol(Type type, String name) {
        super(type, name);
    }

    @Override
    public String toString() {
        return String.format("%s %s", type, name);
    }
}
