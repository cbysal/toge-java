package compile.syntax.ast;

import compile.symbol.DataSymbol;
import compile.symbol.GlobalSymbol;
import compile.symbol.Type;
import compile.symbol.Value;

import java.util.List;

public record VarExpAST(DataSymbol symbol, List<ExpAST> dimensions) implements ExpAST {
    @Override
    public Value calc() {
        if (symbol instanceof GlobalSymbol globalSymbol) {
            if (dimensions.isEmpty()) {
                if (globalSymbol.getType() == Type.FLOAT)
                    return new Value(globalSymbol.getFloat());
                return new Value(globalSymbol.getInt());
            }
            if (globalSymbol.getDimensionSize() != dimensions.size())
                throw new RuntimeException();
            int offset = 0;
            int[] sizes = globalSymbol.getSizes();
            for (int i = 0; i < dimensions.size(); i++)
                offset += sizes[i] * dimensions.get(i).calc().getInt();
            if (globalSymbol.getType() == Type.FLOAT)
                return new Value(globalSymbol.getFloat(offset));
            return new Value(globalSymbol.getInt(offset));
        }
        throw new RuntimeException("Can not calculate symbol: " + symbol.getName());
    }

    public boolean isSingle() {
        return dimensions.isEmpty();
    }

    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "VarExp " + symbol);
        if (dimensions != null)
            dimensions.forEach(dimension -> dimension.print(depth + 1));
    }
}
