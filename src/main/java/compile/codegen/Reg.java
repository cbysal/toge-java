package compile.codegen;

import compile.symbol.Type;

public abstract class Reg {
    protected final Type type;

    protected Reg(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }
}
