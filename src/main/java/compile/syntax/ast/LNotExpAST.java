package compile.syntax.ast;

import compile.symbol.Type;
import compile.symbol.Value;

public class LNotExpAST implements ExpAST {
    public final ExpAST next;

    public LNotExpAST(ExpAST next) {
        this.next = next;
    }

    @Override
    public Value calc() {
        Value nVal = next.calc();
        if (nVal.getType() == Type.FLOAT)
            return new Value(nVal.getFloat() == 0.0f);
        return new Value(nVal.getInt() == 0);
    }
}
