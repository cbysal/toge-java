package compile.syntax.ast;

import compile.symbol.ConstSymbol;

public record ConstDefAST(ConstSymbol symbol) implements CompUnitAST, StmtAST {
    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "ConstDef " + symbol);
    }
}
