package compile.symbol;

import java.util.List;

public class LocalSymbol extends DataSymbol {

    LocalSymbol(Type type, String name) {
        super(type, name);
    }

    LocalSymbol(Type type, String name, List<Integer> dimensions) {
        super(type, name, dimensions);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(type.toString().toLowerCase()).append(' ').append(name);
        dimensions.forEach(dimension -> builder.append('[').append(dimension).append(']'));
        return builder.toString();
    }
}
