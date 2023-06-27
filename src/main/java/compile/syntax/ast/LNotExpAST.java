package compile.syntax.ast;

import compile.symbol.Type;
import compile.symbol.Value;

public record LNotExpAST(ExpAST next) implements ExpAST {
    @Override
    public Value calc() {
        Value nVal = next.calc();
        if (nVal.getType() == Type.FLOAT)
            return new Value(nVal.getFloat() == 0.0f);
        return new Value(nVal.getInt() == 0);
    }

    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "LNotExp");
        next.print(depth + 1);
    }
}
