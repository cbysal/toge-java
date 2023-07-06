package compile.codegen.virgen;

import common.Pair;
import compile.codegen.virgen.vir.*;
import compile.symbol.DataSymbol;
import compile.symbol.GlobalSymbol;
import compile.symbol.Type;
import compile.syntax.ast.*;

import java.util.*;

public class VIRGenerator {
    private boolean isProcessed = false;
    private final RootAST rootAST;
    private final Map<String, GlobalSymbol> consts = new HashMap<>();
    private final Map<String, GlobalSymbol> globals = new HashMap<>();
    private final Map<String, VirtualFunction> funcs = new HashMap<>();
    private VirtualFunction curFunc;
    private Block curBlock, trueBlock, falseBlock, retBlock;
    private final Deque<Block> continueStack = new ArrayDeque<>();
    private final Deque<Block> breakStack = new ArrayDeque<>();

    public VIRGenerator(RootAST rootAST) {
        this.rootAST = rootAST;
    }

    private void checkIfIsProcessed() {
        if (isProcessed)
            return;
        isProcessed = true;
        parseRoot(rootAST);
    }

    public Map<String, GlobalSymbol> getConsts() {
        checkIfIsProcessed();
        return consts;
    }

    public Map<String, VirtualFunction> getFuncs() {
        checkIfIsProcessed();
        return funcs;
    }

    public Map<String, GlobalSymbol> getGlobals() {
        checkIfIsProcessed();
        return globals;
    }

    private void parseAssignStmt(AssignStmtAST assignStmt) {
        Pair<DataSymbol, List<VIRItem>> lValUnit = parseLVal(assignStmt.lVal());
        VReg rReg = parseExp(assignStmt.rVal());
        rReg = typeConversion(rReg, lValUnit.first().getType());
        if (lValUnit.second() == null)
            curBlock.add(new StoreVIR(lValUnit.first(), rReg));
        else
            curBlock.add(new StoreVIR(lValUnit.first(), lValUnit.second(), rReg));
    }

    private VReg parseBinaryExp(BinaryExpAST binaryExp) {
        VReg lReg = parseExp(binaryExp.left());
        VReg rReg = parseExp(binaryExp.right());
        Type targetType = automaticTypePromotion(lReg.getType(), rReg.getType());
        VReg result = new VReg(targetType);
        lReg = typeConversion(lReg, targetType);
        rReg = typeConversion(rReg, targetType);
        BinaryVIR binaryVIR = new BinaryVIR(switch (binaryExp.op()) {
            case ADD -> BinaryVIR.Type.ADD;
            case SUB -> BinaryVIR.Type.SUB;
            case MUL -> BinaryVIR.Type.MUL;
            case DIV -> BinaryVIR.Type.DIV;
            case MOD -> BinaryVIR.Type.MOD;
        }, result, lReg, rReg);
        curBlock.add(binaryVIR);
        return result;
    }

    private void parseBlankStmt() {
    }

    private void parseBlockStmt(BlockStmtAST blockStmt) {
        blockStmt.stmts().forEach(this::parseStmt);
    }

    private void parseBreakStmt() {
        curBlock.add(new JVIR(breakStack.peek()));
    }

    private void parseCmpCond(CmpExpAST cmpCond) {
        VReg lReg = parseExp(cmpCond.left());
        VReg rReg = parseExp(cmpCond.right());
        Type targetType = automaticTypePromotion(lReg.getType(), rReg.getType());
        lReg = typeConversion(lReg, targetType);
        rReg = typeConversion(rReg, targetType);
        curBlock.add(new BVIR(switch (cmpCond.op()) {
            case EQ -> BVIR.Type.EQ;
            case GE -> BVIR.Type.GE;
            case GT -> BVIR.Type.GT;
            case LE -> BVIR.Type.LE;
            case LT -> BVIR.Type.LT;
            case NE -> BVIR.Type.NE;
        }, lReg, rReg, trueBlock, falseBlock));
    }

    private VReg parseCmpExp(CmpExpAST cmpExp) {
        VReg lReg = parseExp(cmpExp.left());
        VReg rReg = parseExp(cmpExp.right());
        Type targetType = automaticTypePromotion(lReg.getType(), rReg.getType());
        VReg result = new VReg(Type.INT);
        lReg = typeConversion(lReg, targetType);
        rReg = typeConversion(rReg, targetType);
        curBlock.add(new BinaryVIR(switch (cmpExp.op()) {
            case EQ -> BinaryVIR.Type.EQ;
            case GE -> BinaryVIR.Type.GE;
            case GT -> BinaryVIR.Type.GT;
            case LE -> BinaryVIR.Type.LE;
            case LT -> BinaryVIR.Type.LT;
            case NE -> BinaryVIR.Type.NE;
        }, result, lReg, rReg));
        return result;
    }

    private void parseCond(ExpAST root) {
        if (root instanceof BinaryExpAST || root instanceof FuncCallExpAST || root instanceof VarExpAST) {
            parseValueCond(root);
            return;
        }
        if (root instanceof CmpExpAST eqNeExp) {
            parseCmpCond(eqNeExp);
            return;
        }
        if (root instanceof FloatLitExpAST floatLitExp) {
            curBlock.add(new JVIR(floatLitExp.value() != 0.0f ? trueBlock : falseBlock));
            return;
        }
        if (root instanceof IntLitExpAST intLitExp) {
            curBlock.add(new JVIR(intLitExp.value() != 0 ? trueBlock : falseBlock));
            return;
        }
        if (root instanceof LAndExpAST lAndExp) {
            parseLAndCond(lAndExp);
            return;
        }
        if (root instanceof LOrExpAST lOrExp) {
            parseLOrCond(lOrExp);
            return;
        }
        if (root instanceof UnaryExpAST unaryExp) {
            parseUnaryCond(unaryExp);
            return;
        }
        if (root instanceof LNotExpAST lNotExpAST) {
            parseLNotCond(lNotExpAST);
            return;
        }
        throw new RuntimeException();
    }

    private void parseLNotCond(LNotExpAST lNotCond) {
        Block trueBlock = this.trueBlock;
        Block falseBlock = this.falseBlock;
        this.trueBlock = falseBlock;
        this.falseBlock = trueBlock;
        parseCond(lNotCond.next());
    }

    private void parseConstDef(ConstDefAST root) {
        GlobalSymbol global = root.symbol();
        consts.put(global.getName(), global);
    }

    private void parseContinueStmt() {
        curBlock.add(new JVIR(continueStack.peek()));
    }

    private VReg parseExp(ExpAST root) {
        if (root instanceof BinaryExpAST addSubExp)
            return parseBinaryExp(addSubExp);
        if (root instanceof CmpExpAST eqNeExp)
            return parseCmpExp(eqNeExp);
        if (root instanceof FloatLitExpAST floatLitExp)
            return parseFloatLitExp(floatLitExp);
        if (root instanceof FuncCallExpAST funcCallExp)
            return parseFuncCallExp(funcCallExp);
        if (root instanceof IntLitExpAST intLitExp)
            return parseIntLitExp(intLitExp);
        if (root instanceof UnaryExpAST unaryExp)
            return parseUnaryExp(unaryExp);
        if (root instanceof LNotExpAST lNotExpAST)
            return parseLNotExp(lNotExpAST);
        if (root instanceof VarExpAST varExp)
            return parseVarExp(varExp);
        throw new RuntimeException();
    }

    private VReg parseLNotExp(LNotExpAST lNotExp) {
        VReg nReg = parseExp(lNotExp.next());
        VReg result = new VReg(Type.INT);
        curBlock.add(new UnaryVIR(UnaryVIR.Type.L_NOT, result, nReg));
        return result;
    }

    private void parseExpStmt(ExpStmtAST root) {
        parseExp(root.exp());
    }

    private VReg parseFloatLitExp(FloatLitExpAST root) {
        VReg reg = new VReg(Type.FLOAT);
        curBlock.add(new LIVIR(reg, root.value()));
        return reg;
    }

    private VReg parseFuncCallExp(FuncCallExpAST funcCallExp) {
        List<VIRItem> params = new ArrayList<>();
        for (int i = 0; i < funcCallExp.params().size(); i++) {
            ExpAST exp = funcCallExp.params().get(i);
            VReg param = parseExp(exp);
            Type targetType =
                    funcCallExp.func().getParams().get(i).isSingle() && funcCallExp.func().getParams().get(i).getType() == Type.FLOAT ? Type.FLOAT : Type.INT;
            param = typeConversion(param, targetType);
            params.add(param);
        }
        VReg retReg = switch (funcCallExp.func().getType()) {
            case FLOAT -> new VReg(Type.FLOAT);
            case INT -> new VReg(Type.INT);
            case VOID -> null;
        };
        curBlock.add(new CallVIR(funcCallExp.func(), retReg, params));
        return retReg;
    }

    private void parseFuncDef(FuncDefAST funcDef) {
        curFunc = new VirtualFunction(funcDef.decl());
        retBlock = new Block();
        curBlock = new Block();
        curFunc.addBlock(curBlock);
        parseBlockStmt(funcDef.body());
        curFunc.addBlock(retBlock);
        funcs.put(funcDef.decl().getName(), curFunc);
    }

    private void parseGlobalDef(GlobalDefAST root) {
        GlobalSymbol global = root.symbol();
        globals.put(global.getName(), global);
    }

    private void parseIfStmt(IfStmtAST ifStmt) {
        Block trueBlock = new Block();
        Block falseBlock = new Block();
        if (ifStmt.hasElse()) {
            Block ifEndBlock = new Block();
            curFunc.insertBlockAfter(curBlock, trueBlock);
            curFunc.insertBlockAfter(trueBlock, falseBlock);
            curFunc.insertBlockAfter(falseBlock, ifEndBlock);
            this.trueBlock = trueBlock;
            this.falseBlock = falseBlock;
            parseCond(ifStmt.cond());
            curBlock = trueBlock;
            parseStmt(ifStmt.stmt1());
            curBlock.add(new JVIR(ifEndBlock));
            curBlock = falseBlock;
            parseStmt(ifStmt.stmt2());
            curBlock.add(new JVIR(ifEndBlock));
            curBlock = ifEndBlock;
        } else {
            curFunc.insertBlockAfter(curBlock, trueBlock);
            curFunc.insertBlockAfter(trueBlock, falseBlock);
            this.trueBlock = trueBlock;
            this.falseBlock = falseBlock;
            parseCond(ifStmt.cond());
            curBlock = trueBlock;
            parseStmt(ifStmt.stmt1());
            curBlock.add(new JVIR(falseBlock));
            curBlock = falseBlock;
        }
    }

    private VReg parseIntLitExp(IntLitExpAST root) {
        VReg reg = new VReg(Type.INT);
        curBlock.add(new LIVIR(reg, root.value()));
        return reg;
    }

    private void parseLAndCond(LAndExpAST lAndCond) {
        Block lBlock = curBlock;
        Block rBlock = new Block();
        curFunc.insertBlockAfter(lBlock, rBlock);
        Block trueBlock = this.trueBlock;
        Block falseBlock = this.falseBlock;
        this.curBlock = lBlock;
        this.trueBlock = rBlock;
        this.falseBlock = falseBlock;
        parseCond(lAndCond.left());
        this.curBlock = rBlock;
        this.trueBlock = trueBlock;
        this.falseBlock = falseBlock;
        parseCond(lAndCond.right());
    }

    private void parseLocalDef(LocalDefAST root) {
        curFunc.addLocal(root.symbol());
    }

    private void parseLOrCond(LOrExpAST lOrExp) {
        Block lBlock = curBlock;
        Block rBlock = new Block();
        curFunc.insertBlockAfter(lBlock, rBlock);
        Block trueBlock = this.trueBlock;
        Block falseBlock = this.falseBlock;
        this.curBlock = lBlock;
        this.trueBlock = trueBlock;
        this.falseBlock = rBlock;
        parseCond(lOrExp.left());
        this.curBlock = rBlock;
        this.trueBlock = trueBlock;
        this.falseBlock = falseBlock;
        parseCond(lOrExp.right());
    }

    private Pair<DataSymbol, List<VIRItem>> parseLVal(LValAST lVal) {
        if (lVal.isSingle())
            return new Pair<>(lVal.symbol(), null);
        List<VIRItem> dimensions = new ArrayList<>();
        for (ExpAST dimension : lVal.dimensions()) {
            VReg reg = parseExp(dimension);
            dimensions.add(reg);
        }
        return new Pair<>(lVal.symbol(), dimensions);
    }

    private void parseRetStmt(RetStmtAST retStmt) {
        if (retStmt.value() == null) {
            curBlock.add(new JVIR(retBlock));
            return;
        }
        VReg retReg = parseExp(retStmt.value());
        retReg = typeConversion(retReg, curFunc.getRetVal().getType());
        curBlock.add(new MovVIR(curFunc.getRetVal(), retReg));
        curBlock.add(new JVIR(retBlock));
    }

    private void parseRoot(RootAST root) {
        root.compUnits().forEach(this::visitCompUnit);
    }

    private void visitCompUnit(CompUnitAST compUnit) {
        if (compUnit instanceof ConstDefAST constDef) {
            parseConstDef(constDef);
            return;
        }
        if (compUnit instanceof GlobalDefAST globalDef) {
            parseGlobalDef(globalDef);
            return;
        }
        if (compUnit instanceof FuncDefAST funcDef) {
            parseFuncDef(funcDef);
            return;
        }
        throw new RuntimeException();
    }

    private void parseStmt(StmtAST root) {
        if (root instanceof AssignStmtAST assignStmt) {
            parseAssignStmt(assignStmt);
            return;
        }
        if (root instanceof BlankStmt) {
            parseBlankStmt();
            return;
        }
        if (root instanceof BlockStmtAST blockStmt) {
            parseBlockStmt(blockStmt);
            return;
        }
        if (root instanceof BreakStmtAST) {
            parseBreakStmt();
            return;
        }
        if (root instanceof ConstDefAST constDef) {
            parseConstDef(constDef);
            return;
        }
        if (root instanceof ContinueStmtAST) {
            parseContinueStmt();
            return;
        }
        if (root instanceof ExpStmtAST expStmt) {
            parseExpStmt(expStmt);
            return;
        }
        if (root instanceof IfStmtAST ifStmt) {
            parseIfStmt(ifStmt);
            return;
        }
        if (root instanceof LocalDefAST localDef) {
            parseLocalDef(localDef);
            return;
        }
        if (root instanceof RetStmtAST retStmt) {
            parseRetStmt(retStmt);
            return;
        }
        if (root instanceof WhileStmtAST whileStmt) {
            parseWhileStmt(whileStmt);
            return;
        }
        throw new RuntimeException();
    }

    private void parseUnaryCond(UnaryExpAST unaryCond) {
        switch (unaryCond.op()) {
            case F2I, I2F -> {
                VReg source = parseExp(unaryCond.next());
                VReg result = new VReg(Type.INT);
                curBlock.add(new UnaryVIR(switch (unaryCond.op()) {
                    case F2I -> UnaryVIR.Type.F2I;
                    case I2F -> UnaryVIR.Type.I2F;
                    default -> throw new RuntimeException();
                }, result, source));
                VReg zero = new VReg(Type.INT);
                curBlock.add(new LIVIR(zero, 0));
                curBlock.add(new BVIR(BVIR.Type.NE, result, zero, trueBlock, falseBlock));
            }
            case NEG -> parseCond(unaryCond.next());
        }
    }

    private VReg parseUnaryExp(UnaryExpAST unaryExp) {
        VReg nVal = parseExp(unaryExp.next());
        return switch (unaryExp.op()) {
            case F2I -> typeConversion(nVal, Type.INT);
            case I2F -> typeConversion(nVal, Type.FLOAT);
            case NEG -> {
                VReg result = new VReg(nVal.getType());
                curBlock.add(new UnaryVIR(UnaryVIR.Type.NEG, result, nVal));
                yield result;
            }
        };
    }

    private void parseValueCond(ExpAST root) {
        VReg result = parseExp(root);
        VReg zero = new VReg(Type.INT);
        curBlock.add(new LIVIR(zero, 0));
        curBlock.add(new BVIR(BVIR.Type.NE, result, zero, trueBlock, falseBlock));
    }

    private VReg parseVarExp(VarExpAST varExp) {
        VReg result =
                new VReg(varExp.dimensions().size() == varExp.symbol().getDimensionSize() && varExp.symbol().getType() == Type.FLOAT ? Type.FLOAT : Type.INT);
        if (varExp.isSingle()) {
            curBlock.add(new LoadVIR(result, varExp.symbol()));
            return result;
        }
        List<VIRItem> dimensions = new ArrayList<>();
        for (ExpAST dimension : varExp.dimensions()) {
            VReg reg = parseExp(dimension);
            if (reg.getType() == Type.FLOAT) {
                VReg newReg = new VReg(Type.INT);
                curBlock.add(new UnaryVIR(UnaryVIR.Type.F2I, newReg, reg));
                reg = newReg;
            }
            dimensions.add(reg);
        }
        curBlock.add(new LoadVIR(result, dimensions, varExp.symbol()));
        return result;
    }

    private void parseWhileStmt(WhileStmtAST whileStmt) {
        Block entryBlock = new Block();
        Block loopBlock = new Block();
        Block endBlock = new Block();
        curFunc.insertBlockAfter(curBlock, entryBlock);
        curFunc.insertBlockAfter(entryBlock, loopBlock);
        curFunc.insertBlockAfter(loopBlock, endBlock);
        continueStack.push(entryBlock);
        breakStack.push(endBlock);
        curBlock.add(new JVIR(entryBlock));
        curBlock = entryBlock;
        trueBlock = loopBlock;
        falseBlock = endBlock;
        parseCond(whileStmt.cond());
        curBlock = loopBlock;
        parseStmt(whileStmt.body());
        curBlock.add(new JVIR(entryBlock));
        curBlock = endBlock;
        continueStack.pop();
        breakStack.pop();
    }

    private static Type automaticTypePromotion(Type type1, Type type2) {
        return type1 == Type.FLOAT || type2 == Type.FLOAT ? Type.FLOAT : Type.INT;
    }

    private VReg typeConversion(VReg reg, Type targetType) {
        if (reg.getType() == targetType)
            return reg;
        if (reg.getType() == Type.FLOAT && targetType == Type.INT) {
            VReg newReg = new VReg(Type.INT);
            curBlock.add(new UnaryVIR(UnaryVIR.Type.F2I, newReg, reg));
            reg = newReg;
        }
        if (reg.getType() == Type.INT && targetType == Type.FLOAT) {
            VReg newReg = new VReg(Type.FLOAT);
            curBlock.add(new UnaryVIR(UnaryVIR.Type.I2F, newReg, reg));
            reg = newReg;
        }
        return reg;
    }

}
