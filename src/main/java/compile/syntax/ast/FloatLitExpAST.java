package compile.syntax.ast;

import compile.symbol.Value;

public record FloatLitExpAST(float value) implements ExpAST {
    @Override
    public Value calc() {
        return new Value(value);
    }

    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "FloatLit " + value);
    }
}
