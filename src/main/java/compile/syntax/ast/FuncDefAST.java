package compile.syntax.ast;

import compile.symbol.FuncSymbol;

public record FuncDefAST(FuncSymbol decl, BlockStmtAST body) implements CompUnitAST {
    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "FuncDef " + decl);
        body.print(depth + 1);
    }
}
