package compile.syntax.pass;

import compile.symbol.GlobalSymbol;
import compile.syntax.ast.*;

public class ConstantFolding extends Pass {
    public ConstantFolding(RootAST rootAST) {
        super(rootAST);
    }

    private boolean modified = false;

    @Override
    public boolean run() {
        for (CompUnitAST compUnit : rootAST)
            if (compUnit instanceof FuncDefAST funcDef)
                visit(funcDef.body);
        return modified;
    }

    private void visit(BlockStmtAST blockStmt) {
        for (StmtAST stmt : blockStmt) {
            if (stmt instanceof IfStmtAST ifStmt) {
                visit(ifStmt.cond);
                continue;
            }
            if (stmt instanceof WhileStmtAST whileStmt){
                visit(whileStmt.cond);
                continue;
            }
        }
    }

    private void visit(ExpAST expStmt) {
        if (expStmt instanceof BinaryExpAST binaryExp) {
            if (binaryExp.left instanceof VarExpAST varExp && varExp.symbol instanceof GlobalSymbol global && global.isConst()) {
                if (global.isSingle()) {
                    binaryExp.left = switch (global.getType()) {
                        case FLOAT -> new FloatLitExpAST(global.getFloat());
                        case INT -> new IntLitExpAST(global.getInt());
                        default -> throw new IllegalStateException("Unexpected value: " + global.getType());
                    };
                    modified = true;
                }
            }
            if (binaryExp.right instanceof VarExpAST varExp && varExp.symbol instanceof GlobalSymbol global && global.isConst()) {
                if (global.isSingle()) {
                    binaryExp.right = switch (global.getType()) {
                        case FLOAT -> new FloatLitExpAST(global.getFloat());
                        case INT -> new IntLitExpAST(global.getInt());
                        default -> throw new IllegalStateException("Unexpected value: " + global.getType());
                    };
                    modified = true;
                }
            }
            return;
        }
        if (expStmt instanceof CmpExpAST cmpExp) {
            if (cmpExp.left instanceof VarExpAST varExp && varExp.symbol instanceof GlobalSymbol global && global.isConst()) {
                if (global.isSingle()) {
                    cmpExp.left = switch (global.getType()) {
                        case FLOAT -> new FloatLitExpAST(global.getFloat());
                        case INT -> new IntLitExpAST(global.getInt());
                        default -> throw new IllegalStateException("Unexpected value: " + global.getType());
                    };
                    modified = true;
                }
            }
            if (cmpExp.right instanceof VarExpAST varExp && varExp.symbol instanceof GlobalSymbol global && global.isConst()) {
                if (global.isSingle()) {
                    cmpExp.right = switch (global.getType()) {
                        case FLOAT -> new FloatLitExpAST(global.getFloat());
                        case INT -> new IntLitExpAST(global.getInt());
                        default -> throw new IllegalStateException("Unexpected value: " + global.getType());
                    };
                    modified = true;
                }
            }
            return;
        }
        if (expStmt instanceof UnaryExpAST unaryExp) {
            visit(unaryExp.next);
            return;
        }
    }
}
