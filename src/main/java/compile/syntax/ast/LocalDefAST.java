package compile.syntax.ast;

import compile.symbol.LocalSymbol;

public record LocalDefAST(LocalSymbol symbol) implements StmtAST {
    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "LocalDef " + symbol);
    }
}
