package compile.syntax.ast;

public class ExpStmtAST implements StmtAST {
    public final ExpAST exp;

    public ExpStmtAST(ExpAST exp) {
        this.exp = exp;
    }

    public ExpStmtAST copy() {
        return new ExpStmtAST(exp.copy());
    }
}
