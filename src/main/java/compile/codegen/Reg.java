package compile.codegen;

import compile.symbol.Type;

public abstract class Reg {
    protected final Type type;
    protected final int size;

    protected Reg(Type type, int size) {
        this.type = type;
        this.size = size;
    }

    public Type getType() {
        return type;
    }

    public int getSize() {
        return size;
    }
}
