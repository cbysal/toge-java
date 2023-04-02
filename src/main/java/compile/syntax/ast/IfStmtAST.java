package compile.syntax.ast;

public record IfStmtAST(ExpAST cond, StmtAST stmt1, StmtAST stmt2) implements StmtAST {
    public boolean hasElse() {
        return stmt2 != null;
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
