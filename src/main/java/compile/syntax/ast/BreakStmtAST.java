package compile.syntax.ast;

public class BreakStmtAST implements StmtAST {
    @Override
    public BreakStmtAST copy() {
        return new BreakStmtAST();
    }
}
