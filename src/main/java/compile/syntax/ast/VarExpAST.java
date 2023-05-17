package compile.syntax.ast;

import compile.symbol.DataSymbol;
import compile.symbol.InitializedDataSymbol;

import java.util.List;

public record VarExpAST(DataSymbol symbol, List<ExpAST> dimensions) implements ExpAST {
    @Override
    public Number calc() {
        if (symbol instanceof InitializedDataSymbol symbol) {
            if (dimensions.isEmpty()) {
                if (symbol.isFloat()) {
                    return symbol.getFloat();
                }
                return symbol.getInt();
            }
            if (symbol.getDimensionSize() != dimensions.size()) {
                throw new RuntimeException();
            }
            int offset = 0;
            List<Integer> dimensionSizes = symbol.getDimensions();
            for (int i = 0; i < dimensions.size(); i++) {
                offset = offset * dimensionSizes.get(i) + dimensions.get(i).calc().intValue();
            }
            if (symbol.isFloat()) {
                return symbol.getFloat(offset);
            }
            return symbol.getInt(offset);
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
