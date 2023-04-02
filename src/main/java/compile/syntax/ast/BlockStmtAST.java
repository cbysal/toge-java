package compile.syntax.ast;

import java.util.ArrayList;

public class BlockStmtAST extends ArrayList<StmtAST> implements StmtAST {
    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "BlockStmt");
        forEach(stmt -> stmt.print(depth + 1));
    }
}
