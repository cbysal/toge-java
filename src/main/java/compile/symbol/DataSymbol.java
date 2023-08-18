package compile.symbol;

import java.util.List;

public abstract class DataSymbol extends Symbol {
    protected final List<Integer> dimensions;
    private final int size;
    private final int[] sizes;

    DataSymbol(Type type, String name) {
        this(type, name, List.of());
    }

    DataSymbol(Type type, String name, List<Integer> dimensions) {
        super(type, name);
        this.dimensions = dimensions;
        this.sizes = new int[dimensions.size()];
        if (sizes.length == 0) {
            this.size = 4;
        } else {
            sizes[sizes.length - 1] = 4;
            for (int i = dimensions.size() - 1; i > 0; i--)
                sizes[i - 1] = sizes[i] * dimensions.get(i);
            this.size = dimensions.get(0) < 0 ? -1 : sizes[0] * dimensions.get(0);
        }
    }

    public List<Integer> getDimensions() {
        return dimensions;
    }

    public int getDimensionSize() {
        return dimensions.size();
    }

    public int[] getSizes() {
        return sizes;
    }

    public boolean isSingle() {
        return dimensions.isEmpty();
    }

    public int size() {
        if (size < 0)
            throw new RuntimeException();
        return size;
    }
}
