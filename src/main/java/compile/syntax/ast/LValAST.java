package compile.syntax.ast;

import compile.symbol.DataSymbol;

import java.util.List;

public class LValAST implements AST {
    private final DataSymbol symbol;
    private final List<ExpAST> dimensions;

    public LValAST(DataSymbol symbol) {
        this(symbol, List.of());
    }

    public LValAST(DataSymbol symbol, List<ExpAST> dimensions) {
        this.symbol = symbol;
        this.dimensions = dimensions;
    }

    public List<ExpAST> getDimensions() {
        return dimensions;
    }

    public DataSymbol getSymbol() {
        return symbol;
    }

    public boolean isSingle() {
        return dimensions.isEmpty();
    }

    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "LVal " + symbol);
        if (dimensions != null) {
            dimensions.forEach(dimension -> dimension.print(depth + 1));
        }
    }
}
