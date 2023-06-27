package compile.syntax.ast;

public record WhileStmtAST(ExpAST cond, StmtAST body) implements StmtAST {
    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "WhileStmt");
        System.out.println("  ".repeat(depth + 1) + "Cond");
        cond.print(depth + 2);
        System.out.println("  ".repeat(depth + 1) + "Body");
        body.print(depth + 2);
    }
}
