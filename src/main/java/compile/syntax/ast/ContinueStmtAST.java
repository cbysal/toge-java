package compile.syntax.ast;

public record ContinueStmtAST() implements StmtAST {
    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "ContinueStmt");
    }
}
