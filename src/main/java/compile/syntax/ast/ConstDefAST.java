package compile.syntax.ast;

import compile.symbol.GlobalSymbol;

public record ConstDefAST(GlobalSymbol symbol) implements CompUnitAST, StmtAST {
    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "ConstDef " + symbol);
    }
}
