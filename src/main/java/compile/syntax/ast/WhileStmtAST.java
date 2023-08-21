package compile.syntax.ast;

public class WhileStmtAST implements StmtAST {
    public final ExpAST cond;
    public final StmtAST body;

    public WhileStmtAST(ExpAST cond, StmtAST body) {
        this.cond = cond;
        this.body = body;
    }

    public WhileStmtAST copy() {
        return new WhileStmtAST(cond.copy(), body.copy());
    }
}
