package compile.syntax.ast;

import compile.symbol.Value;

public interface ExpAST extends AST {
    Value calc();
}
