package compile.syntax.ast;

import compile.symbol.DataSymbol;
import compile.symbol.GlobalSymbol;

import java.util.List;

public record VarExpAST(DataSymbol symbol, List<ExpAST> dimensions) implements ExpAST {
    @Override
    public Number calc() {
        GlobalSymbol global = (GlobalSymbol) symbol;
        if (dimensions.isEmpty()) {
            return global.getValue();
        }
        if (global.getDimensions().size() != dimensions.size()) {
            throw new RuntimeException();
        }
        int offset = 0;
        List<Integer> dimensionSizes = global.getDimensions();
        for (int i = 0; i < dimensions.size(); i++) {
            offset = offset * dimensionSizes.get(i) + dimensions.get(i).calc().intValue();
        }
        return global.getValue(offset);
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
