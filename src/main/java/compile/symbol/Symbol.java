package compile.symbol;

import compile.vir.type.Type;
import compile.vir.value.Value;

public abstract class Symbol extends Value {
    protected final String name;

    Symbol(Type type, String name) {
        super(type);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
