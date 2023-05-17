package compile.symbol;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class FuncSymbol extends Symbol {
    private final boolean hasRet;
    private final List<ParamSymbol> params = new ArrayList<>();

    FuncSymbol(String name) {
        this(false, false, name);
    }

    FuncSymbol(boolean isFloat, String name) {
        this(true, isFloat, name);
    }

    private FuncSymbol(boolean hasRet, boolean isFloat, String name) {
        super(isFloat, name);
        this.hasRet = hasRet;
    }

    public boolean hasRet() {
        return hasRet;
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
        builder.append(hasRet ? (isFloat ? "float " : "int ") : "void ").append(name);
        StringJoiner joiner = new StringJoiner(", ", "(", ")");
        params.forEach(param -> joiner.add(param.toString()));
        builder.append(joiner);
        return builder.toString();
    }
}
