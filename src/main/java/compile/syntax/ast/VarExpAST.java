package compile.syntax.ast;

import compile.symbol.DataSymbol;
import compile.symbol.InitializedDataSymbol;
import compile.symbol.Value;

import java.util.List;

public record VarExpAST(DataSymbol symbol, List<ExpAST> dimensions) implements ExpAST {
    @Override
    public Value calc() {
        if (symbol instanceof InitializedDataSymbol symbol) {
            if (dimensions == null) {
                if (symbol.isFloat()) {
                    return new Value(symbol.getFloat());
                }
                return new Value(symbol.getInt());
            }
            if (symbol.getDimensionSize() != dimensions.size()) {
                throw new RuntimeException();
            }
            int offset = 0;
            List<Integer> dimensionSizes = symbol.getDimensions();
            for (int i = 0; i < dimensions.size(); i++) {
                offset = offset * dimensionSizes.get(i) + dimensions.get(i).calc().getInt();
            }
            if (symbol.isFloat()) {
                return new Value(symbol.getFloat(offset));
            }
            return new Value(symbol.getInt(offset));
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
