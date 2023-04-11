package compile.llvm.ir;

import compile.llvm.ir.type.Type;

public abstract class Value {
    protected final Type type;
    protected final String name;

    protected Value(Type type) {
        this(type, null);
    }

    protected Value(Type type, String name) {
        this.type = type;
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public abstract String getTag();

    public final String getRet() {
        return String.format("%s %s", type, getTag());
    }
}
