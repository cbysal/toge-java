package compile.vir.contant;

import compile.vir.type.Type;

import java.util.List;

public class ConstantArray extends Constant {
    private final List<Constant> values;

    public ConstantArray(Type type, List<Constant> values) {
        super(type);
        this.values = values;
    }

    public List<Constant> getValues() {
        return values;
    }

    @Override
    public String getName() {
        return String.format("%s %s", type, values);
    }

    @Override
    public String toString() {
        return String.format("%s %s", type, values);
    }
}
