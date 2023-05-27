package compile.symbol;

import compile.llvm.ir.type.Type;

public abstract class Symbol {
    final Type type;
    final String name;

    Symbol(Type type, String name) {
        this.type = type;
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}
