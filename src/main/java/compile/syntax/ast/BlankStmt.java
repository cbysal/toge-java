package compile.syntax.ast;

public record BlankStmt() implements StmtAST {
    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "BlankStmt");
    }
}
