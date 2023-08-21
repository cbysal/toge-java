package compile.syntax.pass;

import compile.symbol.LocalSymbol;
import compile.symbol.Type;
import compile.syntax.ast.*;

public class UnrollStandardFor extends Pass {
    private int testVar;
    private boolean modified;

    public UnrollStandardFor(RootAST rootAST) {
        super(rootAST);
    }

    @Override
    public boolean run() {
        return visit(rootAST);
    }

    private boolean visit(RootAST root) {
        for (CompUnitAST compUnit : root)
            if (compUnit instanceof FuncDefAST funcDef)
                visit(funcDef.body);
        return modified;
    }

    private void visit(BlockStmtAST blockStmt) {
        for (int i = 0; i < blockStmt.size() - 1; i++) {
            StmtAST stmt1 = blockStmt.get(i);
            StmtAST stmt2 = blockStmt.get(i + 1);
            if (stmt1 instanceof AssignStmtAST assignStmt && stmt2 instanceof WhileStmtAST whileStmt) {
                if (assignStmt.lVal.symbol instanceof LocalSymbol loopVar && loopVar.isSingle() && loopVar.getType() == Type.INT && assignStmt.rVal instanceof IntLitExpAST startIntLit) {
                    int startVal = startIntLit.value;
                    if (whileStmt.cond instanceof CmpExpAST cmpExp) {
                        if (cmpExp.op == CmpExpAST.Op.LT && cmpExp.left instanceof VarExpAST varExp && varExp.symbol == loopVar && cmpExp.right instanceof IntLitExpAST stopIntLit) {
                            int stopVal = stopIntLit.value;
                            if (whileStmt.body instanceof BlockStmtAST whileBody && whileBody.get(whileBody.size() - 1) instanceof AssignStmtAST selfAddStmt && selfAddStmt.lVal.symbol == loopVar && selfAddStmt.rVal instanceof BinaryExpAST addStmt && addStmt.op == BinaryExpAST.Op.ADD && addStmt.left instanceof VarExpAST addLeft && addLeft.symbol == loopVar && addStmt.right instanceof IntLitExpAST stepExp) {
                                testVar = 0;
                                test(whileBody, loopVar);
                                if (testVar == 1) {
                                    int stepVal = stepExp.value;
                                    if ((stopVal - startVal) / stepVal > 100)
                                        continue;
                                    blockStmt.remove(i + 1);
                                    for (int j = 0; j < (stopVal - startVal) / stepVal; j++)
                                        blockStmt.add(i + 1 + j, whileBody.copy());
                                    modified = true;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void test(BlockStmtAST blockStmt, LocalSymbol loopVar) {
        for (StmtAST stmt : blockStmt) {
            if (stmt instanceof AssignStmtAST assignStmt)
                if (assignStmt.lVal.symbol == loopVar)
                    testVar++;
            if (stmt instanceof BreakStmtAST)
                testVar++;
            if (stmt instanceof BlockStmtAST block)
                test(block, loopVar);
            if (stmt instanceof ContinueStmtAST)
                testVar++;
            if (stmt instanceof IfStmtAST ifStmt) {
                if (ifStmt.stmt1 instanceof BlockStmtAST stmt1)
                    test(stmt1, loopVar);
                if (ifStmt.hasElse() && ifStmt.stmt2 instanceof BlockStmtAST stmt2)
                    test(stmt2, loopVar);
            }
            if (stmt instanceof RetStmtAST)
                testVar++;
            if (stmt instanceof WhileStmtAST)
                testVar++;
        }
    }
}
