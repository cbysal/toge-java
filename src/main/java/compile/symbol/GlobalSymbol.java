package compile.symbol;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class GlobalSymbol extends DataSymbol {
    private final boolean isConst;
    private final Number value;
    private final Map<Integer, Number> values;

    GlobalSymbol(boolean isConst, String name, Number value) {
        super(value instanceof Float, name, List.of());
        this.isConst = isConst;
        this.value = value;
        this.values = null;
    }

    GlobalSymbol(boolean isConst, boolean isFloat, String name, List<Integer> dimensions, Map<Integer, Number> values) {
        super(isFloat, name, dimensions);
        this.isConst = isConst;
        this.value = 0;
        this.values = values;
    }

    public boolean isConst() {
        return isConst;
    }

    public Number getValue() {
        return value;
    }

    public Number getValue(int index) {
        return values.getOrDefault(index, isFloat ? 0.0f : 0);
    }

    public Map<Integer, Number> getValues() {
        return values;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(isConst ? "const " : "global ").append(isFloat ? "float " : "int ").append(name);
        if (dimensions.isEmpty()) {
            builder.append(" = ").append(value);
            return builder.toString();
        }
        dimensions.forEach(dimension -> builder.append('[').append(dimension).append(']'));
        builder.append(" = ");
        StringJoiner joiner = new StringJoiner(", ", "{", "}");
        values.forEach((index, value) -> joiner.add(index + ": " + value));
        builder.append(joiner);
        return builder.toString();
    }
}
