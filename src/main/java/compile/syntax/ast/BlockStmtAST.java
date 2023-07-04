package compile.syntax.ast;

import java.util.List;

public record BlockStmtAST(List<StmtAST> stmts) implements StmtAST {
}
