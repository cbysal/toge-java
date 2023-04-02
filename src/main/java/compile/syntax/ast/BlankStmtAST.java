package compile.syntax.ast;

public class BlankStmtAST implements StmtAST {
    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "BlankStmt");
    }
}
