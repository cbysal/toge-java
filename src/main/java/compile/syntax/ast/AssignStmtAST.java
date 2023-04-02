package compile.syntax.ast;

public class AssignStmtAST implements StmtAST {
    private final LValAST lVal;
    private final ExpAST rVal;

    public AssignStmtAST(LValAST lVal, ExpAST rVal) {
        this.lVal = lVal;
        this.rVal = rVal;
    }

    public LValAST getLVal() {
        return lVal;
    }

    public ExpAST getRVal() {
        return rVal;
    }

    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "AssignStmt");
        lVal.print(depth + 1);
        rVal.print(depth + 1);
    }
}
