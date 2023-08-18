package compile.syntax.ast;

import compile.symbol.GlobalSymbol;

public class ConstDefAST extends CompUnitAST implements StmtAST {
    public final GlobalSymbol symbol;

    public ConstDefAST(GlobalSymbol symbol) {
        this.symbol = symbol;
    }
}
