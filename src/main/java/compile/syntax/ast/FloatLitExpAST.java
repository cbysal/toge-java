package compile.syntax.ast;

import compile.symbol.Value;

public class FloatLitExpAST implements ExpAST {
    private final float value;

    public FloatLitExpAST(float value) {
        this.value = value;
    }

    @Override
    public Value calc() {
        return new Value(value);
    }

    public float getValue() {
        return value;
    }

    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "FloatLit " + value);
    }
}
