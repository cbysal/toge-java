package compile.syntax.ast;

public record RetStmtAST(ExpAST value) implements StmtAST {
    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "RetStmt");
        if (value != null) {
            value.print(depth + 1);
        }
    }
}
