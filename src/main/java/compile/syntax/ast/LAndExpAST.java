package compile.syntax.ast;

import compile.symbol.Type;
import compile.symbol.Value;

public class LAndExpAST implements ExpAST {
    public final ExpAST left;
    public final ExpAST right;

    public LAndExpAST(ExpAST left, ExpAST right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public Value calc() {
        Value lVal = left.calc();
        Value rVal = right.calc();
        if (lVal.getType() == Type.FLOAT && rVal.getType() == Type.FLOAT)
            return new Value(lVal.getFloat() != 0.0f && rVal.getFloat() != 0.0f);
        if (lVal.getType() == Type.FLOAT)
            return new Value(lVal.getFloat() != 0.0f && rVal.getInt() != 0);
        if (rVal.getType() == Type.FLOAT)
            return new Value(lVal.getInt() != 0 && rVal.getFloat() != 0.0f);
        return new Value(lVal.getInt() != 0 && rVal.getInt() != 0);
    }
}
