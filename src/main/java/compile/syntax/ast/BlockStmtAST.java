package compile.syntax.ast;

import java.util.List;

public record BlockStmtAST(List<StmtAST> stmts) implements StmtAST {
    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "BlockStmt");
        stmts.forEach(stmt -> stmt.print(depth + 1));
    }
}
