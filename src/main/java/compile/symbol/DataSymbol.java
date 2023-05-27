package compile.symbol;

import compile.llvm.ir.type.Type;

public abstract class DataSymbol extends Symbol {
    DataSymbol(Type type, String name) {
        super(type, name);
    }
}
