package compile.syntax.ast;

import compile.llvm.ir.type.ArrayType;
import compile.llvm.ir.type.Type;
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
        int offset = 0;
        Type type = global.getType();
        for (ExpAST dimension : dimensions) {
            ArrayType arrayType = (ArrayType) type;
            offset = offset * arrayType.dimension() + dimension.calc().intValue();
            type = arrayType.base();
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
