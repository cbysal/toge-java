package compile.symbol;

import compile.codegen.virgen.vir.type.Type;

import java.util.ArrayList;
import java.util.List;

public class LocalSymbol extends DataSymbol {

    public LocalSymbol(Type type, String name) {
        super(type, name);
    }

    public LocalSymbol(Type type, String name, List<Integer> dimensions) {
        super(type, name, dimensions);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(type.toString().toLowerCase()).append(' ').append(name);
        dimensions.forEach(dimension -> builder.append('[').append(dimension).append(']'));
        return builder.toString();
    }

    @Override
    public LocalSymbol clone() {
        List<Integer> newDimensions = new ArrayList<>(dimensions);
        return new LocalSymbol(type, name, newDimensions);
    }
}
