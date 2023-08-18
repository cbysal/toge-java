package compile.syntax.pass;

import compile.syntax.ast.*;

public class RemoveDuplicatedASTsPass extends Pass {
    public RemoveDuplicatedASTsPass(RootAST rootAST) {
        super(rootAST);
    }

    @Override
    public boolean optimize() {
        return visit(rootAST);
    }

    private boolean visit(BlockStmtAST blockStmt) {
        boolean modified = false;
        for (int i = 0; i < blockStmt.size(); i++) {
            StmtAST stmt = blockStmt.get(i);
            if (stmt instanceof BreakStmtAST || stmt instanceof ContinueStmtAST || stmt instanceof RetStmtAST) {
                while (blockStmt.get(blockStmt.size() - 1) != stmt) {
                    blockStmt.remove(blockStmt.size() - 1);
                    modified = true;
                }
                break;
            }
            if (stmt instanceof IfStmtAST ifStmt) {
                if (ifStmt.stmt1 instanceof BlockStmtAST innerBlockStmt)
                    modified |= visit(innerBlockStmt);
                if (ifStmt.hasElse() && ifStmt.stmt2 instanceof BlockStmtAST innerBlockStmt)
                    modified |= visit(innerBlockStmt);
                continue;
            }
            if (stmt instanceof WhileStmtAST whileStmt) {
                if (whileStmt.body instanceof BlockStmtAST innerBlockStmt)
                    modified |= visit(innerBlockStmt);
                continue;
            }
        }
        return modified;
    }

    private boolean visit(FuncDefAST funcDef) {
        return visit(funcDef.body);
    }

    private boolean visit(RootAST root) {
        boolean modified = false;
        for (CompUnitAST compUnit : root)
            if (compUnit instanceof FuncDefAST funcDef)
                modified |= visit(funcDef);
        return modified;
    }
}
