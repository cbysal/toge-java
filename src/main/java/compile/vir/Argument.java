package compile.vir;

import compile.vir.type.Type;
import compile.vir.value.Value;

public class Argument extends Value {
    private final String name;

    public Argument(Type type, String name) {
        super(type);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return String.format("%s %s", type, name);
    }
}
