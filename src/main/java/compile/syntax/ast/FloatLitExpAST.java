package compile.syntax.ast;

import compile.symbol.Value;

public class FloatLitExpAST implements ExpAST {
    public final float value;

    public FloatLitExpAST(float value) {
        this.value = value;
    }

    @Override
    public Value calc() {
        return new Value(value);
    }
}
