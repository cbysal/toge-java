package compile.syntax.ast;

public class ContinueStmtAST implements StmtAST {
    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "ContinueStmt");
    }
}
