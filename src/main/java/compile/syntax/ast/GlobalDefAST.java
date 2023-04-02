package compile.syntax.ast;

import compile.symbol.GlobalSymbol;

public class GlobalDefAST implements CompUnitAST {
    private final GlobalSymbol symbol;

    public GlobalDefAST(GlobalSymbol symbol) {
        this.symbol = symbol;
    }

    public GlobalSymbol getSymbol() {
        return symbol;
    }

    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "GlobalDef " + symbol);
    }
}
