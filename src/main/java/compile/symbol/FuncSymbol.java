package compile.symbol;

import java.util.ArrayList;
import java.util.List;

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

    public void addParam(ParamSymbol param) {
        params.add(param);
    }

    public List<ParamSymbol> getParams() {
        return params;
    }

    public boolean hasRet() {
        return hasRet;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(hasRet ? (isFloat ? "float " : "int ") : "void ").append(name).append('(');
        boolean isFirst = true;
        for (ParamSymbol param : params) {
            if (!isFirst) {
                builder.append(", ");
            }
            isFirst = false;
            builder.append(param);
        }
        builder.append(')');
        return builder.toString();
    }
}
