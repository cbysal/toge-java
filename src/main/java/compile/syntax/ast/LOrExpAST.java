package compile.syntax.ast;

import compile.symbol.Type;
import compile.symbol.Value;

public class LOrExpAST implements ExpAST {
    public final ExpAST left;
    public final ExpAST right;

    public LOrExpAST(ExpAST left, ExpAST right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public Value calc() {
        Value lVal = left.calc();
        Value rVal = right.calc();
        if (lVal.getType() == Type.FLOAT && rVal.getType() == Type.FLOAT)
            return new Value(lVal.floatValue() != 0.0f || rVal.floatValue() != 0.0f);
        if (lVal.getType() == Type.FLOAT)
            return new Value(lVal.floatValue() != 0.0f || rVal.intValue() != 0);
        if (rVal.getType() == Type.FLOAT)
            return new Value(lVal.intValue() != 0 || rVal.floatValue() != 0.0f);
        return new Value(lVal.intValue() != 0 || rVal.intValue() != 0);
    }
}
