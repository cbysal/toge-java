package compile.syntax.ast;

import compile.symbol.DataSymbol;
import compile.symbol.GlobalSymbol;

import java.util.List;

public record VarExpAST(DataSymbol symbol, List<ExpAST> dimensions) implements ExpAST {
    @Override
    public Number calc() {
        if (symbol instanceof GlobalSymbol symbol) {
            if (dimensions.isEmpty()) {
                return symbol.getValue();
            }
            if (symbol.getDimensions().size() != dimensions.size()) {
                throw new RuntimeException();
            }
            int offset = 0;
            List<Integer> dimensionSizes = symbol.getDimensions();
            for (int i = 0; i < dimensions.size(); i++) {
                offset = offset * dimensionSizes.get(i) + dimensions.get(i).calc().intValue();
            }
            return symbol.getValue(offset);
        }
        throw new RuntimeException("Can not calculate symbol: " + symbol.getName());
    }

    public boolean isSingle() {
        return dimensions == null;
    }

    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "VarExp " + symbol);
        if (dimensions != null) {
            dimensions.forEach(dimension -> dimension.print(depth + 1));
        }
    }
}
