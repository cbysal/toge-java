package compile.symbol;

import java.util.List;

public class LocalSymbol extends DataSymbol {

    LocalSymbol(boolean isFloat, String name) {
        super(isFloat, name);
    }

    LocalSymbol(boolean isFloat, String name, List<Integer> dimensions) {
        super(isFloat, name, dimensions);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(isFloat() ? "float " : "int ");
        builder.append(name);
        dimensions.forEach(dimension -> builder.append('[').append(dimension).append(']'));
        return builder.toString();
    }
}
