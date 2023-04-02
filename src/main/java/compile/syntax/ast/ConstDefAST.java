package compile.syntax.ast;

import compile.symbol.ConstSymbol;

public class ConstDefAST implements CompUnitAST, StmtAST {
    private final ConstSymbol symbol;

    public ConstDefAST(ConstSymbol symbol) {
        this.symbol = symbol;
    }

    public ConstSymbol getSymbol() {
        return symbol;
    }

    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "ConstDef " + symbol);
    }
}
