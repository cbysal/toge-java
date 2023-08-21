package compile.syntax.ast;

import compile.symbol.FuncSymbol;
import compile.symbol.Value;

import java.util.ArrayList;
import java.util.List;

public class FuncCallExpAST implements ExpAST {
    public final FuncSymbol func;
    public final List<ExpAST> params;

    public FuncCallExpAST(FuncSymbol func, List<ExpAST> params) {
        this.func = func;
        this.params = params;
    }

    @Override
    public Value calc() {
        throw new RuntimeException();
    }

    public FuncCallExpAST copy() {
        List<ExpAST> newParams = new ArrayList<>();
        for (ExpAST param : params)
            newParams.add(param.copy());
        return new FuncCallExpAST(func, newParams);
    }
}
