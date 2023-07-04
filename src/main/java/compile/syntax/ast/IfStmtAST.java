package compile.syntax.ast;

public record IfStmtAST(ExpAST cond, StmtAST stmt1, StmtAST stmt2) implements StmtAST {
    public boolean hasElse() {
        return stmt2 != null;
    }
}
