package compile.syntax.ast;

public final class IfStmtAST implements StmtAST {
    public final ExpAST cond;
    public final StmtAST stmt1;
    public final StmtAST stmt2;

    public IfStmtAST(ExpAST cond, StmtAST stmt1, StmtAST stmt2) {
        this.cond = cond;
        this.stmt1 = stmt1;
        this.stmt2 = stmt2;
    }

    public boolean hasElse() {
        return stmt2 != null;
    }
}
