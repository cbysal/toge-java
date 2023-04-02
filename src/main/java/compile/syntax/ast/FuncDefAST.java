package compile.syntax.ast;

import compile.symbol.FuncSymbol;

public class FuncDefAST implements CompUnitAST {
    private final FuncSymbol decl;
    private final BlockStmtAST body;

    public FuncDefAST(FuncSymbol decl, BlockStmtAST body) {
        this.decl = decl;
        this.body = body;
    }

    public FuncSymbol getDecl() {
        return decl;
    }

    public BlockStmtAST getBody() {
        return body;
    }

    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "FuncDef " + decl);
        body.print(depth + 1);
    }
}
