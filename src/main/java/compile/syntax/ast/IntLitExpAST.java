package compile.syntax.ast;

import compile.symbol.Value;

public record IntLitExpAST(int value) implements ExpAST {
    @Override
    public Value calc() {
        return new Value(value);
    }

    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "IntLit " + value);
    }
}
