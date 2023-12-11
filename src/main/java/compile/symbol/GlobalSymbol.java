package compile.symbol;

import compile.vir.type.BasicType;
import compile.vir.type.Type;

import java.util.List;
import java.util.Map;

public class GlobalSymbol extends Symbol {
    private final boolean isConst;
    private final List<Integer> dimensions;
    private final int size;
    private final int[] sizes;
    private final int value;
    private final Map<Integer, Integer> values;

    public GlobalSymbol(boolean isConst, Type type, String name, float value) {
        super(type, name);
        this.isConst = isConst;
        this.dimensions = List.of();
        this.size = 4;
        this.sizes = new int[0];
        this.value = Float.floatToIntBits(value);
        this.values = null;
    }

    public GlobalSymbol(boolean isConst, Type type, String name, int value) {
        super(type, name);
        this.isConst = isConst;
        this.dimensions = List.of();
        this.sizes = new int[0];
        this.size = 4;
        this.value = value;
        this.values = null;
    }

    public GlobalSymbol(boolean isConst, Type type, String name, List<Integer> dimensions, Map<Integer, Integer> values) {
        super(type, name);
        this.isConst = isConst;
        this.dimensions = dimensions;
        this.sizes = new int[dimensions.size()];
        if (sizes.length == 0) {
            this.size = 4;
        } else {
            sizes[sizes.length - 1] = 4;
            for (int i = dimensions.size() - 1; i > 0; i--)
                sizes[i - 1] = sizes[i] * dimensions.get(i);
            this.size = dimensions.getFirst() < 0 ? -1 : sizes[0] * dimensions.getFirst();
        }
        this.value = 0;
        this.values = values;
    }

    public boolean isConst() {
        return isConst;
    }

    public List<Integer> getDimensions() {
        return dimensions;
    }

    public int getDimensionSize() {
        return dimensions.size();
    }

    public int size() {
        return size;
    }

    public int[] getSizes() {
        return sizes;
    }

    public boolean isSingle() {
        return dimensions.isEmpty();
    }

    public boolean isInBss() {
        return values.isEmpty();
    }

    public float getFloat() {
        if (!dimensions.isEmpty())
            throw new RuntimeException();
        return Float.intBitsToFloat(value);
    }

    public float getFloat(int index) {
        if (dimensions.isEmpty())
            throw new RuntimeException();
        return Float.intBitsToFloat(values.getOrDefault(index, 0));
    }

    public int getInt() {
        if (!dimensions.isEmpty())
            throw new RuntimeException();
        return value;
    }

    public int getInt(int index) {
        if (dimensions.isEmpty())
            throw new RuntimeException();
        return values.getOrDefault(index, 0);
    }

    public Map<Integer, Integer> getValues() {
        return values;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (type == BasicType.FLOAT) {
            builder.append("float ").append(name);
            if (dimensions.isEmpty())
                builder.append(" = ").append(Float.intBitsToFloat(value));
            else {
                dimensions.forEach(dimension -> builder.append('[').append(dimension).append(']'));
                builder.append(" = {");
                boolean isFirst = true;
                for (Map.Entry<Integer, Integer> value : values.entrySet()) {
                    if (!isFirst)
                        builder.append(", ");
                    isFirst = false;
                    builder.append(value.getKey()).append(": ").append(Float.intBitsToFloat(value.getValue()));
                }
                builder.append('}');
            }
        } else {
            builder.append("int ").append(name);
            if (dimensions.isEmpty())
                builder.append(" = ").append(value);
            else {
                dimensions.forEach(dimension -> builder.append('[').append(dimension).append(']'));
                builder.append(" = {");
                boolean isFirst = true;
                for (Map.Entry<Integer, Integer> value : values.entrySet()) {
                    if (!isFirst)
                        builder.append(", ");
                    isFirst = false;
                    builder.append(value.getKey()).append(": ").append(value.getValue());
                }
                builder.append('}');
            }
        }
        return builder.toString();
    }
}
