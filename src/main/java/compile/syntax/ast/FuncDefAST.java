package compile.syntax.ast;

import compile.symbol.FuncSymbol;

public record FuncDefAST(FuncSymbol decl, BlockStmtAST body) implements CompUnitAST {
}
