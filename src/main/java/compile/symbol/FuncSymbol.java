package compile.symbol;

import compile.vir.type.Type;

import java.util.ArrayList;
import java.util.List;

public class FuncSymbol extends Symbol {
    private final List<ParamSymbol> params = new ArrayList<>();

    FuncSymbol(Type type, String name) {
        super(type, name);
    }

    public void addParam(ParamSymbol param) {
        params.add(param);
    }

    public List<ParamSymbol> getParams() {
        return params;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(type.toString().toLowerCase()).append(' ').append(name).append('(');
        boolean isFirst = true;
        for (ParamSymbol param : params) {
            if (!isFirst)
                builder.append(", ");
            isFirst = false;
            builder.append(param);
        }
        builder.append(')');
        return builder.toString();
    }
}
