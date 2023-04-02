package compile.syntax.ast;

public class BreakStmtAST implements StmtAST {
    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "BreakStmt");
    }
}
