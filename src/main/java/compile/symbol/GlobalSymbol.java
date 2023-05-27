package compile.symbol;

import compile.llvm.ir.type.BasicType;
import compile.llvm.ir.type.Type;

import java.util.Map;
import java.util.StringJoiner;

public class GlobalSymbol extends DataSymbol {
    private final Number value;
    private final Map<Integer, Number> values;

    GlobalSymbol(Type type, String name, Number value) {
        super(type, name);
        this.value = value;
        this.values = null;
    }

    GlobalSymbol(Type type, String name, Map<Integer, Number> values) {
        super(type, name);
        this.value = 0;
        this.values = values;
    }

    public Number getValue() {
        return value;
    }

    public Number getValue(int index) {
        return values.getOrDefault(index, 0);
    }

    public Map<Integer, Number> getValues() {
        return values;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(type).append(' ').append(name).append(" = ");
        if (type instanceof BasicType) {
            builder.append(value);
            return builder.toString();
        }
        StringJoiner joiner = new StringJoiner(", ", "{", "}");
        values.forEach((index, value) -> joiner.add(index + ": " + value));
        builder.append(joiner);
        return builder.toString();
    }
}
