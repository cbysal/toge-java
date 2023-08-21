package compile.syntax.ast;

import compile.symbol.Value;

public interface ExpAST {
    Value calc();

    ExpAST copy();
}
