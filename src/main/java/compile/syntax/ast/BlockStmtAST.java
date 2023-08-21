package compile.syntax.ast;

import java.util.ArrayList;

public class BlockStmtAST extends ArrayList<StmtAST> implements StmtAST {
    @Override
    public BlockStmtAST copy() {
        BlockStmtAST newBlockStmt = new BlockStmtAST();
        for (StmtAST stmt : this)
            newBlockStmt.add(stmt.copy());
        return newBlockStmt;
    }
}
