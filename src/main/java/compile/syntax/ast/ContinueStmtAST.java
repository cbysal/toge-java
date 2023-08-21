package compile.syntax.ast;

public class ContinueStmtAST implements StmtAST {
    @Override
    public ContinueStmtAST copy() {
        return new ContinueStmtAST();
    }
}
