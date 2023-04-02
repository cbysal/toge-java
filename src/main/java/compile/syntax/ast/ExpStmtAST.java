package compile.syntax.ast;

public record ExpStmtAST(ExpAST exp) implements StmtAST {
    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "ExpStmt");
        exp.print(depth + 1);
    }
}
