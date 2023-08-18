package compile.syntax.ast;

import compile.symbol.DataSymbol;

import java.util.List;

public class LValAST extends AST {
    public final DataSymbol symbol;
    public final List<ExpAST> dimensions;

    public LValAST(DataSymbol symbol, List<ExpAST> dimensions) {
        this.symbol = symbol;
        this.dimensions = dimensions;
    }

    public boolean isSingle() {
        return dimensions.isEmpty();
    }
}
