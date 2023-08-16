package compile.syntax.pass;

import compile.syntax.ast.*;

import java.util.List;

public class ExpandNestedBlocksPass extends Pass {
    public ExpandNestedBlocksPass(RootAST rootAST) {
        super(rootAST);
    }

    @Override
    public boolean optimize() {
        return visit(rootAST);
    }

    private boolean visit(BlockStmtAST blockStmt) {
        boolean modified = false;
        List<StmtAST> stmts = blockStmt.stmts();
        for (int i = 0; i < stmts.size(); i++) {
            StmtAST stmt = stmts.get(i);
            if (stmt instanceof BlockStmtAST nestedBlockStmt) {
                stmts.remove(i);
                stmts.addAll(i, nestedBlockStmt.stmts());
                i--;
                modified = true;
                continue;
            }
            if (stmt instanceof IfStmtAST ifStmt) {
                if (ifStmt.stmt1() instanceof BlockStmtAST innerBlockStmt)
                    modified |= visit(innerBlockStmt);
                if (ifStmt.hasElse() && ifStmt.stmt2() instanceof BlockStmtAST innerBlockStmt)
                    modified |= visit(innerBlockStmt);
                continue;
            }
            if (stmt instanceof WhileStmtAST whileStmt) {
                if (whileStmt.body() instanceof BlockStmtAST innerBlockStmt)
                    modified |= visit(innerBlockStmt);
                continue;
            }
        }
        return modified;
    }

    private boolean visit(FuncDefAST funcDef) {
        return visit(funcDef.body());
    }

    private boolean visit(RootAST root) {
        boolean modified = false;
        for (CompUnitAST compUnit : root.compUnits())
            if (compUnit instanceof FuncDefAST funcDef)
                modified |= visit(funcDef);
        return modified;
    }
}