package compile.syntax.ast;

public record BreakStmtAST() implements StmtAST {
    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "BreakStmt");
    }
}
