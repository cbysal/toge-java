package compile.syntax.ast;

public record AssignStmtAST(LValAST lVal, ExpAST rVal) implements StmtAST {
}
