package compile.symbol;

import compile.codegen.virgen.vir.type.Type;

import java.util.List;

public class ParamSymbol extends DataSymbol {

    ParamSymbol(Type type, String name) {
        super(type, name);
    }

    ParamSymbol(Type type, String name, List<Integer> dimensions) {
        super(type, name, dimensions);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(type.toString().toLowerCase()).append(' ').append(name);
        dimensions.forEach(dimension -> builder.append('[').append(dimension == -1 ? "" : dimension).append(']'));
        return builder.toString();
    }
}
