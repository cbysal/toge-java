package compile.syntax.ast;

import compile.symbol.LocalSymbol;

public record LocalDefAST(LocalSymbol symbol) implements StmtAST {
}
