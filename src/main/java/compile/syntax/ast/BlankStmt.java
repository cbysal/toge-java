package compile.syntax.ast;

public final class BlankStmt implements StmtAST {
    @Override
    public BlankStmt copy() {
        return new BlankStmt();
    }
}
