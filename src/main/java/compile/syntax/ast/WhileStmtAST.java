package compile.syntax.ast;

public class WhileStmtAST implements StmtAST {
    private final ExpAST cond;
    private final StmtAST body;

    public WhileStmtAST(ExpAST cond, StmtAST body) {
        this.cond = cond;
        this.body = body;
    }

    public StmtAST getBody() {
        return body;
    }

    public ExpAST getCond() {
        return cond;
    }

    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "WhileStmt");
        System.out.println("  ".repeat(depth + 1) + "Cond");
        cond.print(depth + 2);
        System.out.println("  ".repeat(depth + 1) + "Body");
        body.print(depth + 2);
    }
}
