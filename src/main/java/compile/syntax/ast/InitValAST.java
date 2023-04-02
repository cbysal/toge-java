package compile.syntax.ast;

import compile.symbol.Value;

import java.util.ArrayList;

public class InitValAST extends ArrayList<ExpAST> implements ExpAST {
    @Override
    public Value calc() {
        return null;
    }

    @Override
    public void print(int depth) {
        throw new RuntimeException();
    }
}
