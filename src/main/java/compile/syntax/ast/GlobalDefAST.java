package compile.syntax.ast;

import compile.symbol.GlobalSymbol;

public class GlobalDefAST extends CompUnitAST {
    public final GlobalSymbol symbol;

    public GlobalDefAST(GlobalSymbol symbol) {
        this.symbol = symbol;
    }

    public GlobalDefAST copy() {
        return new GlobalDefAST(symbol);
    }
}
