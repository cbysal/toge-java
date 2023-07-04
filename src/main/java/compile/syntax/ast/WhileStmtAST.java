package compile.syntax.ast;

public record WhileStmtAST(ExpAST cond, StmtAST body) implements StmtAST {
}
