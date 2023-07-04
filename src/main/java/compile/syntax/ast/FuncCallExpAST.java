package compile.syntax.ast;

import compile.symbol.FuncSymbol;
import compile.symbol.Value;

import java.util.List;

public record FuncCallExpAST(FuncSymbol func, List<ExpAST> params) implements ExpAST {
    @Override
    public Value calc() {
        throw new RuntimeException();
    }
}
