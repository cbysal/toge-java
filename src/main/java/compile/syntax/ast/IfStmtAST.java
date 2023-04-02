package compile.syntax.ast;

public class IfStmtAST implements StmtAST {
    private final ExpAST cond;
    private final StmtAST stmt1, stmt2;

    public IfStmtAST(ExpAST cond, StmtAST stmt1) {
        this(cond, stmt1, null);
    }

    public IfStmtAST(ExpAST cond, StmtAST stmt1, StmtAST stmt2) {
        this.cond = cond;
        this.stmt1 = stmt1;
        this.stmt2 = stmt2;
    }

    public boolean hasElse() {
        return stmt2 != null;
    }

    public ExpAST getCond() {
        return cond;
    }

    public StmtAST getStmt1() {
        return stmt1;
    }

    public StmtAST getStmt2() {
        return stmt2;
    }

    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "IfStmt");
        System.out.println("  ".repeat(depth + 1) + "Cond");
        cond.print(depth + 2);
        System.out.println("  ".repeat(depth + 1) + "Stmt1");
        stmt1.print(depth + 2);
        if (stmt2 != null) {
            System.out.println("  ".repeat(depth + 1) + "Stmt2");
            stmt2.print(depth + 2);
        }
    }
}
