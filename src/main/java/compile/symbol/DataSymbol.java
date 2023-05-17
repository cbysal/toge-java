package compile.symbol;

import java.util.List;

public abstract class DataSymbol extends Symbol {
    final List<Integer> dimensions;

    DataSymbol(boolean isFloat, String name, List<Integer> dimensions) {
        super(isFloat, name);
        this.dimensions = dimensions;
    }

    public List<Integer> getDimensions() {
        return dimensions;
    }

    public boolean isSingle() {
        return dimensions.isEmpty();
    }
}
