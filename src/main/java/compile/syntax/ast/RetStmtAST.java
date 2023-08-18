package compile.syntax.ast;

public class RetStmtAST implements StmtAST {
    public final ExpAST value;

    public RetStmtAST(ExpAST value) {
        this.value = value;
    }
}
