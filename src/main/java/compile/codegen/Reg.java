package compile.codegen;

import compile.vir.type.Type;
import compile.vir.value.Value;

public abstract class Reg extends Value {
    protected final Type type;
    protected final int size;

    protected Reg(Type type, int size) {
        super(type);
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
