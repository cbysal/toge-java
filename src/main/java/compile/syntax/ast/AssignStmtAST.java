package compile.syntax.ast;

public record AssignStmtAST(LValAST lVal, ExpAST rVal) implements StmtAST {
    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "AssignStmt");
        lVal.print(depth + 1);
        rVal.print(depth + 1);
    }
}
