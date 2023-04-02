package compile.syntax.ast;

import compile.symbol.LocalSymbol;

public class LocalDefAST implements StmtAST {
    private final LocalSymbol symbol;

    public LocalDefAST(LocalSymbol symbol) {
        this.symbol = symbol;
    }

    public LocalSymbol getSymbol() {
        return symbol;
    }

    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "LocalDef " + symbol);
    }
}
