package compile.syntax.ast;

import compile.symbol.Value;

import java.util.ArrayList;

public class InitValAST extends ArrayList<ExpAST> implements ExpAST {
    @Override
    public Value calc() {
        return null;
    }

    public InitValAST copy() {
        InitValAST newInitVal = new InitValAST();
        for (ExpAST exp : this)
            newInitVal.add(exp.copy());
        return newInitVal;
    }
}
