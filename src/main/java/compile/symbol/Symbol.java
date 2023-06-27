package compile.symbol;

public abstract class Symbol {
    protected final Type type;
    protected final String name;

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
