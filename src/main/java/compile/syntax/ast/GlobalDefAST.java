package compile.syntax.ast;

import compile.symbol.GlobalSymbol;

public record GlobalDefAST(GlobalSymbol symbol) implements CompUnitAST {
    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "GlobalDef " + symbol);
    }
}
