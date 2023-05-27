package compile.symbol;

import compile.llvm.ir.type.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

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
        builder.append(type).append(' ').append(name);
        StringJoiner joiner = new StringJoiner(", ", "(", ")");
        params.forEach(param -> joiner.add(param.toString()));
        builder.append(joiner);
        return builder.toString();
    }
}
