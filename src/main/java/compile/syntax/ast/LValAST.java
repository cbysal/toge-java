package compile.syntax.ast;

import compile.symbol.DataSymbol;

import java.util.List;

public record LValAST(DataSymbol symbol, List<ExpAST> dimensions) implements AST {
    public boolean isSingle() {
        return dimensions.isEmpty();
    }

    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "LVal " + symbol);
        if (dimensions != null)
            dimensions.forEach(dimension -> dimension.print(depth + 1));
    }
}
