package compile.symbol;

public abstract class Symbol {
    protected final boolean isFloat;
    protected final String name;

    Symbol(boolean isFloat, String name) {
        this.isFloat = isFloat;
        this.name = name;
    }

    public boolean isFloat() {
        return isFloat;
    }

    public String getName() {
        return name;
    }
}
