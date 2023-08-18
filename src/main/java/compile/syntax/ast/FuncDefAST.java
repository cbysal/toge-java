package compile.syntax.ast;

import compile.symbol.FuncSymbol;

public class FuncDefAST extends CompUnitAST {
    public final FuncSymbol decl;
    public final BlockStmtAST body;

    public FuncDefAST(FuncSymbol decl, BlockStmtAST body) {
        this.decl = decl;
        this.body = body;
    }
}
