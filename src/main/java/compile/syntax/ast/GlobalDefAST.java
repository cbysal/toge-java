package compile.syntax.ast;

import compile.symbol.GlobalSymbol;

public record GlobalDefAST(GlobalSymbol symbol) implements CompUnitAST {
}
