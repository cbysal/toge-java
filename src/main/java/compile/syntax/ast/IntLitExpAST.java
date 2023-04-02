package compile.syntax.ast;

import compile.symbol.Value;

public class IntLitExpAST implements ExpAST {
    private final int value;

    public IntLitExpAST(int value) {
        this.value = value;
    }

    @Override
    public Value calc() {
        return new Value(value);
    }

    public int getValue() {
        return value;
    }

    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "IntLit " + value);
    }
}
