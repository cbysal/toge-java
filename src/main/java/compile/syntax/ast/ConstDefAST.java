package compile.syntax.ast;

import compile.symbol.GlobalSymbol;

public record ConstDefAST(GlobalSymbol symbol) implements CompUnitAST, StmtAST {
}
