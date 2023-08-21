package compile.syntax.ast;

import compile.symbol.Value;

public class IntLitExpAST implements ExpAST {
    public final int value;

    public IntLitExpAST(int value) {
        this.value = value;
    }

    @Override
    public Value calc() {
        return new Value(value);
    }

    public IntLitExpAST copy() {
        return new IntLitExpAST(value);
    }
}
