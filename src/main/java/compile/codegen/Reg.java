package compile.codegen;

import compile.llvm.type.Type;

public abstract class Reg {
    protected final Type type;

    protected Reg(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }
}
