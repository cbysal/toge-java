package compile.syntax.ast;

public class ExpStmtAST implements StmtAST {
    private final ExpAST exp;

    public ExpStmtAST(ExpAST exp) {
        this.exp = exp;
    }

    public ExpAST getExp() {
        return exp;
    }

    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "ExpStmt");
        exp.print(depth + 1);
    }
}
