package compile.syntax.ast;

import compile.symbol.DataSymbol;

import java.util.List;

public record LValAST(DataSymbol symbol, List<ExpAST> dimensions) implements AST {
    public boolean isSingle() {
        return dimensions.isEmpty();
    }
}
