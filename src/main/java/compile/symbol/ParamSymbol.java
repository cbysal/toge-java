package compile.symbol;

import java.util.List;

public class ParamSymbol extends DataSymbol {

    ParamSymbol(boolean isFloat, String name) {
        super(isFloat, name);
    }

    ParamSymbol(boolean isFloat, String name, List<Integer> dimensions) {
        super(isFloat, name, dimensions);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(isFloat() ? "float " : "int ");
        builder.append(name);
        dimensions.forEach(dimension -> builder.append('[').append(dimension == -1 ? "" : dimension).append(']'));
        return builder.toString();
    }
}
