package compile.syntax.ast;

public class AssignStmtAST implements StmtAST {
    public final LValAST lVal;
    public final ExpAST rVal;

    public AssignStmtAST(LValAST lVal, ExpAST rVal) {
        this.lVal = lVal;
        this.rVal = rVal;
    }

    @Override
    public AssignStmtAST copy() {
        return new AssignStmtAST(lVal.copy(), rVal.copy());
    }
}
