package compile.symbol;

import java.util.List;
import java.util.Map;

public abstract class InitializedDataSymbol extends DataSymbol {
    protected final int value;
    protected final Map<Integer, Integer> values;

    InitializedDataSymbol(boolean isFloat, String name, float value) {
        super(isFloat, name);
        this.value = Float.floatToIntBits(value);
        this.values = null;
    }

    InitializedDataSymbol(boolean isFloat, String name, int value) {
        super(isFloat, name);
        this.value = value;
        this.values = null;
    }

    InitializedDataSymbol(boolean isFloat, String name, List<Integer> dimensions, Map<Integer, Integer> values) {
        super(isFloat, name, dimensions);
        this.value = 0;
        this.values = values;
    }

    public float getFloat() {
        if (!isSingle()) {
            throw new RuntimeException();
        }
        return Float.intBitsToFloat(value);
    }

    public float getFloat(int index) {
        if (isSingle()) {
            throw new RuntimeException();
        }
        return Float.intBitsToFloat(values.getOrDefault(index, 0));
    }

    public int getInt() {
        if (!isSingle()) {
            throw new RuntimeException();
        }
        return value;
    }

    public int getInt(int index) {
        if (isSingle()) {
            throw new RuntimeException();
        }
        return values.getOrDefault(index, 0);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (isFloat) {
            builder.append("float ").append(name);
            if (dimensions.isEmpty()) {
                builder.append(" = ").append(Float.intBitsToFloat(value));
            } else {
                dimensions.forEach(dimension -> builder.append('[').append(dimension).append(']'));
                builder.append(" = {");
                boolean isFirst = true;
                for (Map.Entry<Integer, Integer> value : values.entrySet()) {
                    if (!isFirst) {
                        builder.append(", ");
                    }
                    isFirst = false;
                    builder.append(value.getKey()).append(": ").append(Float.intBitsToFloat(value.getValue()));
                }
                builder.append('}');
            }
        } else {
            builder.append("int ").append(name);
            if (dimensions.isEmpty()) {
                builder.append(" = ").append(value);
            } else {
                dimensions.forEach(dimension -> builder.append('[').append(dimension).append(']'));
                builder.append(" = {");
                boolean isFirst = true;
                for (Map.Entry<Integer, Integer> value : values.entrySet()) {
                    if (!isFirst) {
                        builder.append(", ");
                    }
                    isFirst = false;
                    builder.append(value.getKey()).append(": ").append(value.getValue());
                }
                builder.append('}');
            }
        }
        return builder.toString();
    }
}
