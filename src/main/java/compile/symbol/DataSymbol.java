package compile.symbol;

import java.util.List;

public abstract class DataSymbol extends Symbol {
    private final int size;
    final List<Integer> dimensions;

    DataSymbol(boolean isFloat, String name) {
        this(isFloat, name, List.of());
    }

    DataSymbol(boolean isFloat, String name, List<Integer> dimensions) {
        super(isFloat, name);
        this.dimensions = dimensions;
        this.size = dimensions.stream().reduce(4, (i1, i2) -> i1 * i2);
    }

    public List<Integer> getDimensions() {
        return dimensions;
    }

    public int getDimensionSize() {
        return dimensions.size();
    }

    boolean isSingle() {
        return dimensions.isEmpty();
    }

    public int size() {
        return size;
    }
}
