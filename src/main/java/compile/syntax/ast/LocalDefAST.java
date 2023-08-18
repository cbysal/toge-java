package compile.syntax.ast;

import compile.symbol.LocalSymbol;

public class LocalDefAST implements StmtAST {
    public final LocalSymbol symbol;

    public LocalDefAST(LocalSymbol symbol) {
        this.symbol = symbol;
    }
}
