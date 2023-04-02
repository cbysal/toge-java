package compile.syntax.ast;

import compile.symbol.FuncSymbol;
import compile.symbol.Value;

import java.util.List;

public class FuncCallExpAST implements ExpAST {
    private final FuncSymbol func;
    private final List<ExpAST> params;

    public FuncCallExpAST(FuncSymbol func, List<ExpAST> params) {
        this.func = func;
        this.params = params;
    }

    @Override
    public Value calc() {
        throw new RuntimeException();
    }

    public FuncSymbol getFunc() {
        return func;
    }

    public List<ExpAST> getParams() {
        return params;
    }

    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "FuncCallExp " + func);
        params.forEach(param -> param.print(depth + 1));
    }
}
