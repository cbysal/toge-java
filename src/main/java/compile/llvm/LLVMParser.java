package compile.llvm;

import compile.llvm.ir.Module;
import compile.llvm.ir.*;
import compile.llvm.ir.constant.*;
import compile.llvm.ir.instr.*;
import compile.llvm.ir.type.ArrayType;
import compile.llvm.ir.type.BasicType;
import compile.llvm.ir.type.PointerType;
import compile.llvm.ir.type.Type;
import compile.symbol.FuncSymbol;
import compile.symbol.GlobalSymbol;
import compile.symbol.LocalSymbol;
import compile.symbol.ParamSymbol;
import compile.syntax.ast.*;

import java.util.*;

// TODO reconstruct the class
public class LLVMParser {
    private boolean isProcessed;
    private final RootAST root;
    private final Module module = new Module();

    private final SymbolTable symbolTable = new SymbolTable();
    private Function curFunc;
    private BasicBlock allocBlock;
    private BasicBlock paramBlock;
    private BasicBlock curBlock;
    private BasicBlock trueBlock;
    private BasicBlock falseBlock;
    private final Deque<BasicBlock> continueStack = new ArrayDeque<>();
    private final Deque<BasicBlock> breakStack = new ArrayDeque<>();

    public LLVMParser(RootAST root) {
        this.root = root;
        initBuiltInFuncs();
        initSyscalls();
    }

    private void initBuiltInFuncs() {
        Function func;
        // getint
        func = new Function(BasicType.I32, "getint");
        symbolTable.putFirst("getint", func);
        module.addDeclare(func);
        // getch
        func = new Function(BasicType.I32, "getch");
        symbolTable.putFirst("getch", func);
        module.addDeclare(func);
        // getarray
        func = new Function(BasicType.I32, "getarray");
        func.addParam(new Param(new PointerType(BasicType.I32), "a"));
        symbolTable.putFirst("getarray", func);
        module.addDeclare(func);
        // getfloat
        func = new Function(BasicType.FLOAT, "getfloat");
        symbolTable.putFirst("getfloat", func);
        module.addDeclare(func);
        // getfarray
        func = new Function(BasicType.I32, "getfarray");
        func.addParam(new Param(new PointerType(BasicType.FLOAT), "a"));
        symbolTable.putFirst("getfarray", func);
        module.addDeclare(func);
        // putint
        func = new Function(BasicType.VOID, "putint");
        func.addParam(new Param(BasicType.I32, "a"));
        symbolTable.putFirst("putint", func);
        module.addDeclare(func);
        // putch
        func = new Function(BasicType.VOID, "putch");
        func.addParam(new Param(BasicType.I32, "a"));
        symbolTable.putFirst("putch", func);
        module.addDeclare(func);
        // putarray
        func = new Function(BasicType.VOID, "putarray");
        func.addParam(new Param(BasicType.I32, "n"));
        func.addParam(new Param(new PointerType(BasicType.I32), "a"));
        symbolTable.putFirst("putarray", func);
        module.addDeclare(func);
        // putfloat
        func = new Function(BasicType.VOID, "putfloat");
        func.addParam(new Param(BasicType.FLOAT, "a"));
        symbolTable.putFirst("putfloat", func);
        module.addDeclare(func);
        // putfarray
        func = new Function(BasicType.VOID, "putfarray");
        func.addParam(new Param(BasicType.I32, "n"));
        func.addParam(new Param(new PointerType(BasicType.FLOAT), "a"));
        symbolTable.putFirst("putfarray", func);
        module.addDeclare(func);
        // _sysy_starttime
        func = new Function(BasicType.VOID, "_sysy_starttime");
        func.addParam(new Param(BasicType.I32, "lineno"));
        symbolTable.putFirst("_sysy_starttime", func);
        module.addDeclare(func);
        // _sysy_stoptime
        func = new Function(BasicType.VOID, "_sysy_stoptime");
        func.addParam(new Param(BasicType.I32, "lineno"));
        symbolTable.putFirst("_sysy_stoptime", func);
        module.addDeclare(func);
    }

    private void initSyscalls() {
        Function func;
        // memset
        func = new Function(BasicType.VOID, "memset");
        func.addParam(new Param(new PointerType(BasicType.I32), "s"));
        func.addParam(new Param(BasicType.I32, "c"));
        func.addParam(new Param(BasicType.I32, "n"));
        symbolTable.putFirst("memset", func);
        module.addDeclare(func);
    }

    private void checkIfIsProcessed() {
        if (isProcessed) {
            return;
        }
        isProcessed = true;
        visitRoot(root);
    }

    private void visitRoot(RootAST root) {
        root.compUnits().forEach(this::visitCompUnit);
    }

    private void visitCompUnit(CompUnitAST compUnit) {
        if (compUnit instanceof ConstDefAST constDef) {
            visitConstDef(constDef);
            return;
        }
        if (compUnit instanceof GlobalDefAST globalDef) {
            visitGlobalDef(globalDef);
            return;
        }
        if (compUnit instanceof FuncDefAST funcDef) {
            visitFuncDef(funcDef);
            return;
        }
    }

    private void visitConstDef(ConstDefAST constDef) {
        GlobalSymbol constSymbol = constDef.symbol();
        String name = constSymbol.getName();
        Global global;
        if (constSymbol.isSingle()) {
            global = new Global(name, constSymbol.isFloat() ? new FloatConstant(constSymbol.getValue().floatValue())
                    : new I32Constant(constSymbol.getValue().intValue()));
        } else {
            global = parseGlobalSymbol(constSymbol);
        }
        module.addGlobal(global);
        symbolTable.putFirst(name, global);
    }

    private void visitGlobalDef(GlobalDefAST globalDef) {
        GlobalSymbol globalSymbol = globalDef.symbol();
        String name = globalSymbol.getName();
        Global global;
        if (globalSymbol.isSingle()) {
            global = new Global(name, globalSymbol.isFloat() ?
                    new FloatConstant(globalSymbol.getValue().floatValue()) :
                    new I32Constant(globalSymbol.getValue().intValue()));
        } else {
            global = parseGlobalSymbol(globalSymbol);
        }
        module.addGlobal(global);
        symbolTable.putFirst(name, global);
    }

    private void visitLocalDef(LocalDefAST localDef) {
        LocalSymbol localSymbol = localDef.symbol();
        Type type = localSymbol.isFloat() ? BasicType.FLOAT : BasicType.I32;
        List<Integer> dimensions = localSymbol.getDimensions();
        for (int i = dimensions.size() - 1; i >= 0; i--) {
            type = new ArrayType(type, dimensions.get(i));
        }
        AllocInstr allocInstr = new AllocInstr(type);
        allocBlock.addLast(allocInstr);
        symbolTable.putLast(localSymbol.getName(), allocInstr);
    }

    // TODO optimize performance, kill the ugly whole range
    private Global parseGlobalSymbol(GlobalSymbol symbol) {
        List<Integer> dimensions = symbol.getDimensions();
        int total = dimensions.stream().reduce(1, (i1, i2) -> i1 * i2);
        List<Constant> values = new ArrayList<>(total);
        Type type = symbol.isFloat() ? BasicType.FLOAT : BasicType.I32;
        for (int i = 0; i < total; i++) {
            if (symbol.getValues().containsKey(i)) {
                values.add(symbol.isFloat() ? new FloatConstant(symbol.getValue(i).floatValue()) :
                        new I32Constant(symbol.getValue(i).intValue()));
            } else {
                values.add(new ZeroConstant(type));
            }
        }
        for (int i = dimensions.size() - 1; i >= 0; i--) {
            int dimension = dimensions.get(i);
            total /= dimension;
            List<Constant> newValues = new ArrayList<>(total);
            for (int j = 0; j < total; j++) {
                Map<Integer, Value> valueMap = new HashMap<>();
                for (int k = 0; k < dimension; k++) {
                    if (values.get(j * dimension + k) instanceof ZeroConstant) {
                        valueMap.put(k, new ZeroConstant(type));
                    } else {
                        valueMap.put(k, values.get(j * dimension + k));
                    }
                }
                if (valueMap.values().stream().allMatch(value -> value instanceof ZeroConstant)) {
                    newValues.add(new ZeroConstant(new ArrayType(type, dimension)));
                } else {
                    newValues.add(new ArrConstant(type, dimension, valueMap));
                }
            }
            values = newValues;
            type = new ArrayType(type, dimension);
        }
        Constant toWrapValue = values.get(0);
        return new Global(symbol.getName(), toWrapValue);
    }

    private void visitFuncDef(FuncDefAST funcDef) {
        symbolTable.in();
        FuncSymbol funcSymbol = funcDef.decl();
        BasicType retType = BasicType.VOID;
        if (funcSymbol.hasRet()) {
            retType = funcSymbol.isFloat() ? BasicType.FLOAT : BasicType.I32;
        }
        String name = funcSymbol.getName();
        curFunc = new Function(retType, name);
        allocBlock = new BasicBlock(curFunc);
        paramBlock = new BasicBlock(curFunc);
        curBlock = new BasicBlock(curFunc);
        curFunc.addLast(curBlock);
        parseFuncSymbol(funcDef.decl());
        visitBlockStmt(funcDef.body());
        curFunc.getFirst().mergeFirst(paramBlock);
        curFunc.getFirst().mergeFirst(allocBlock);
        if (curFunc.getType() == BasicType.I32) {
            curFunc.getLast().addLast(new RetInstr(new I32Constant(0)));
        } else if (curFunc.getType() == BasicType.VOID) {
            curFunc.getLast().addLast(new RetInstr());
        }
        module.addFunction(curFunc);
        symbolTable.out();
    }

    private void parseFuncSymbol(FuncSymbol funcSymbol) {
        for (ParamSymbol param : funcSymbol.getParams()) {
            parseParamSymbol(param);
        }
    }

    private void parseParamSymbol(ParamSymbol paramSymbol) {
        Type type = paramSymbol.isFloat() ? BasicType.FLOAT : BasicType.I32;
        Param param;
        if (!paramSymbol.isSingle()) {
            List<Integer> dimensions = paramSymbol.getDimensions();
            for (int i = dimensions.size() - 1; i > 0; i--) {
                type = new ArrayType(type, dimensions.get(i));
            }
            type = new PointerType(type);
        }
        param = new Param(type, paramSymbol.getName());
        curFunc.addParam(param);
        AllocInstr alloc = new AllocInstr(type);
        allocBlock.addLast(alloc);
        symbolTable.putLast(paramSymbol.getName(), alloc);
        StoreInstr store = new StoreInstr(param, alloc);
        paramBlock.addLast(store);
    }

    private void visitBlockStmt(BlockStmtAST blockStmt) {
        blockStmt.stmts().forEach(this::visitStmt);
    }

    private void visitStmt(StmtAST stmt) {
        if (stmt instanceof ConstDefAST constDef) {
            visitConstDef(constDef);
            return;
        }
        if (stmt instanceof LocalDefAST localDef) {
            visitLocalDef(localDef);
            return;
        }
        if (stmt instanceof BlockStmtAST blockStmt) {
            symbolTable.in();
            visitBlockStmt(blockStmt);
            symbolTable.out();
            return;
        }
        if (stmt instanceof IfStmtAST ifStmt) {
            visitIfStmt(ifStmt);
            return;
        }
        if (stmt instanceof WhileStmtAST whileStmt) {
            visitWhileStmt(whileStmt);
            return;
        }
        if (stmt instanceof ContinueStmtAST) {
            visitContinueStmt();
            return;
        }
        if (stmt instanceof BreakStmtAST) {
            visitBreakStmt();
            return;
        }
        if (stmt instanceof AssignStmtAST assignStmt) {
            visitAssignStmt(assignStmt);
            return;
        }
        if (stmt instanceof ExpStmtAST expStmt) {
            visitExpStmt(expStmt);
            return;
        }
        if (stmt instanceof RetStmtAST retStmt) {
            visitRetStmt(retStmt);
            return;
        }
    }

    private void visitIfStmt(IfStmtAST ifStmt) {
        BasicBlock trueBlock = new BasicBlock(curFunc);
        BasicBlock falseBlock = new BasicBlock(curFunc);
        if (ifStmt.hasElse()) {
            BasicBlock ifEndBlock = new BasicBlock(curFunc);
            curBlock.insertAfter(trueBlock);
            trueBlock.insertAfter(falseBlock);
            falseBlock.insertAfter(ifEndBlock);
            this.trueBlock = trueBlock;
            this.falseBlock = falseBlock;
            visitCond(ifStmt.cond());
            curBlock = trueBlock;
            visitStmt(ifStmt.stmt1());
            curBlock.addLast(new BranchInstr(ifEndBlock));
            curBlock = falseBlock;
            visitStmt(ifStmt.stmt2());
            curBlock.addLast(new BranchInstr(ifEndBlock));
            curBlock = ifEndBlock;
        } else {
            curBlock.insertAfter(trueBlock);
            trueBlock.insertAfter(falseBlock);
            this.trueBlock = trueBlock;
            this.falseBlock = falseBlock;
            visitCond(ifStmt.cond());
            curBlock = trueBlock;
            visitStmt(ifStmt.stmt1());
            curBlock.addLast(new BranchInstr(falseBlock));
            curBlock = falseBlock;
        }
    }

    private void visitWhileStmt(WhileStmtAST whileStmt) {
        BasicBlock entryBlock = new BasicBlock(curFunc);
        BasicBlock loopBlock = new BasicBlock(curFunc);
        BasicBlock endBlock = new BasicBlock(curFunc);
        curBlock.insertAfter(entryBlock);
        entryBlock.insertAfter(loopBlock);
        loopBlock.insertAfter(endBlock);
        continueStack.push(entryBlock);
        breakStack.push(endBlock);
        curBlock.addLast(new BranchInstr(entryBlock));
        curBlock = entryBlock;
        trueBlock = loopBlock;
        falseBlock = endBlock;
        visitCond(whileStmt.cond());
        curBlock = loopBlock;
        visitStmt(whileStmt.body());
        curBlock.addLast(new BranchInstr(entryBlock));
        curBlock = endBlock;
        continueStack.pop();
        breakStack.pop();
    }

    private void visitContinueStmt() {
        curBlock.addLast(new BranchInstr(continueStack.peek()));
    }

    private void visitBreakStmt() {
        curBlock.addLast(new BranchInstr(breakStack.peek()));
    }

    private void visitCond(ExpAST cond) {
        if (cond instanceof BinaryExpAST binaryExp) {
            BinaryExpAST.Type type = binaryExp.type();
            if (type == BinaryExpAST.Type.L_OR) {
                visitLOrCond(binaryExp);
                return;
            }
            if (type == BinaryExpAST.Type.L_AND) {
                visitLAndCond(binaryExp);
                return;
            }
            if (Set.of(BinaryExpAST.Type.EQ, BinaryExpAST.Type.NE, BinaryExpAST.Type.GE, BinaryExpAST.Type.GT,
                    BinaryExpAST.Type.LE, BinaryExpAST.Type.LT).contains(binaryExp.type())) {
                visitCmpCond(binaryExp);
                return;
            }
        }
        if (cond instanceof UnaryExpAST unaryExp) {
            if (unaryExp.type() == UnaryExpAST.Type.L_NOT) {
                visitLNotCond(unaryExp);
                return;
            }
        }
        Value cVal = visitExp(cond);
        Instr cmpInstr = cVal.getType() == BasicType.I32 ? new CmpInstr(CmpInstr.Op.NE, cVal, new I32Constant(0)) :
                new FcmpInstr(FcmpInstr.Op.UNE, cVal, new FloatConstant(0));
        curBlock.addLast(cmpInstr);
        Instr branchInstr = new BranchInstr(cmpInstr, trueBlock, falseBlock);
        curBlock.addLast(branchInstr);
    }

    private void visitLOrCond(BinaryExpAST lOrExp) {
        BasicBlock lBlock = curBlock;
        BasicBlock rBlock = new BasicBlock(curFunc);
        lBlock.insertAfter(rBlock);
        BasicBlock trueBlock = this.trueBlock;
        BasicBlock falseBlock = this.falseBlock;
        this.curBlock = lBlock;
        this.trueBlock = trueBlock;
        this.falseBlock = rBlock;
        visitCond(lOrExp.left());
        this.curBlock = rBlock;
        this.trueBlock = trueBlock;
        this.falseBlock = falseBlock;
        visitCond(lOrExp.right());
    }

    private void visitLAndCond(BinaryExpAST lAndCond) {
        BasicBlock lBlock = curBlock;
        BasicBlock rBlock = new BasicBlock(curFunc);
        lBlock.insertAfter(rBlock);
        BasicBlock trueBlock = this.trueBlock;
        BasicBlock falseBlock = this.falseBlock;
        this.curBlock = lBlock;
        this.trueBlock = rBlock;
        this.falseBlock = falseBlock;
        visitCond(lAndCond.left());
        this.curBlock = rBlock;
        this.trueBlock = trueBlock;
        this.falseBlock = falseBlock;
        visitCond(lAndCond.right());
    }

    private void visitLNotCond(UnaryExpAST lNotCond) {
        BasicBlock trueBlock = this.trueBlock;
        BasicBlock falseBlock = this.falseBlock;
        this.trueBlock = falseBlock;
        this.falseBlock = trueBlock;
        visitCond(lNotCond.next());
    }

    private void visitCmpCond(BinaryExpAST cmpCond) {
        Value lVal = visitExp(cmpCond.left());
        Value rVal = visitExp(cmpCond.right());
        Type targetType = lVal.getType() == BasicType.FLOAT || rVal.getType() == BasicType.FLOAT ? BasicType.FLOAT :
                BasicType.I32;
        lVal = typeConversion(lVal, targetType);
        rVal = typeConversion(rVal, targetType);
        Instr cmpInstr = targetType == BasicType.I32 ? new CmpInstr(switch (cmpCond.type()) {
            case EQ -> CmpInstr.Op.EQ;
            case NE -> CmpInstr.Op.NE;
            case GE -> CmpInstr.Op.SGE;
            case GT -> CmpInstr.Op.SGT;
            case LE -> CmpInstr.Op.SLE;
            case LT -> CmpInstr.Op.SLT;
            default -> null;
        }, lVal, rVal) : new FcmpInstr(switch (cmpCond.type()) {
            case EQ -> FcmpInstr.Op.OEQ;
            case NE -> FcmpInstr.Op.UNE;
            case GE -> FcmpInstr.Op.OGE;
            case GT -> FcmpInstr.Op.OGT;
            case LE -> FcmpInstr.Op.OLE;
            case LT -> FcmpInstr.Op.OLT;
            default -> null;
        }, lVal, rVal);
        curBlock.addLast(cmpInstr);
        Instr brInstr = new BranchInstr(cmpInstr, trueBlock, falseBlock);
        curBlock.addLast(brInstr);
    }

    private void visitAssignStmt(AssignStmtAST assignStmt) {
        Value lVal = visitLVal(assignStmt.lVal());
        Value rVal = visitExp(assignStmt.rVal());
        if (((PointerType) lVal.getType()).base() != rVal.getType()) {
            rVal = typeConversion(rVal, ((PointerType) lVal.getType()).base());
        }
        curBlock.addLast(new StoreInstr(rVal, lVal));
    }

    private void visitExpStmt(ExpStmtAST expStmt) {
        visitExp(expStmt.exp());
    }

    private Value visitLVal(LValAST lVal) {
        Value ptr = symbolTable.getValue(lVal.symbol().getName());
        // TODO optimize the logic, here must be something wrong
        if (ptr.getType() instanceof PointerType pointerType && pointerType.base() instanceof PointerType) {
            Instr instr = new LoadInstr(ptr);
            curBlock.addLast(instr);
            ptr = instr;
        }
        List<ExpAST> dimensions = lVal.dimensions();
        for (ExpAST dimension : dimensions) {
            Value index = visitExp(dimension);
            Instr ptrInstr;
            if (((PointerType) ptr.getType()).base() instanceof BasicType) {
                ptrInstr = new GetElementPtrInstr(ptr, index);
            } else {
                ptrInstr = new GetElementPtrInstr(ptr, new I32Constant(0), index);
            }
            curBlock.addLast(ptrInstr);
            ptr = ptrInstr;
        }
        return ptr;
    }

    private void visitRetStmt(RetStmtAST retStmt) {
        if (retStmt.value() == null) {
            RetInstr retInstr = new RetInstr();
            curBlock.addLast(retInstr);
            return;
        }
        Value retValue = visitExp(retStmt.value());
        retValue = typeConversion(retValue, curFunc.getType());
        RetInstr retInstr = new RetInstr(retValue);
        curBlock.addLast(retInstr);
    }

    private Value visitExp(ExpAST exp) {
        if (exp instanceof BinaryExpAST binaryExp) {
            return visitBinaryExp(binaryExp);
        }
        if (exp instanceof UnaryExpAST unaryExp) {
            return visitUnaryExp(unaryExp);
        }
        if (exp instanceof VarExpAST varExp) {
            return visitVarExp(varExp);
        }
        if (exp instanceof FuncCallExpAST funcCallExp) {
            return visitFuncCallExp(funcCallExp);
        }
        if (exp instanceof IntLitExpAST intLitExp) {
            return visitIntLitExp(intLitExp);
        }
        if (exp instanceof FloatLitExpAST floatLitExp) {
            return visitFloatLitExp(floatLitExp);
        }
        throw new RuntimeException("Unprocessed ExpAST: " + exp);
    }

    private Value visitBinaryExp(BinaryExpAST binaryExp) {
        Value lVal = visitExp(binaryExp.left());
        Value rVal = visitExp(binaryExp.right());
        Type targetType = lVal.getType() == BasicType.FLOAT || rVal.getType() == BasicType.FLOAT ? BasicType.FLOAT :
                BasicType.I32;
        lVal = typeConversion(lVal, targetType);
        rVal = typeConversion(rVal, targetType);
        Instr instr = switch (binaryExp.type()) {
            case EQ, NE, GE, GT, LE, LT -> targetType == BasicType.I32 ? new CmpInstr(switch (binaryExp.type()) {
                case EQ -> CmpInstr.Op.EQ;
                case NE -> CmpInstr.Op.NE;
                case GE -> CmpInstr.Op.SGE;
                case GT -> CmpInstr.Op.SGT;
                case LE -> CmpInstr.Op.SLE;
                case LT -> CmpInstr.Op.SLT;
                default -> null;
            }, lVal, rVal) : new FcmpInstr(switch (binaryExp.type()) {
                case EQ -> FcmpInstr.Op.OEQ;
                case NE -> FcmpInstr.Op.UNE;
                case GE -> FcmpInstr.Op.OGE;
                case GT -> FcmpInstr.Op.OGT;
                case LE -> FcmpInstr.Op.OLE;
                case LT -> FcmpInstr.Op.OLT;
                default -> null;
            }, lVal, rVal);
            case ADD, SUB, MUL, DIV, MOD -> new BinaryInstr(switch (binaryExp.type()) {
                case ADD -> targetType == BasicType.I32 ? BinaryInstr.Op.ADD : BinaryInstr.Op.FADD;
                case SUB -> targetType == BasicType.I32 ? BinaryInstr.Op.SUB : BinaryInstr.Op.FSUB;
                case DIV -> targetType == BasicType.I32 ? BinaryInstr.Op.SDIV : BinaryInstr.Op.FDIV;
                case MUL -> targetType == BasicType.I32 ? BinaryInstr.Op.MUL : BinaryInstr.Op.FMUL;
                case MOD -> BinaryInstr.Op.SREM;
                default -> throw new IllegalStateException("Unexpected value: " + binaryExp.type());
            }, lVal, rVal);
            default -> throw new IllegalStateException("Unexpected value: " + binaryExp.type());
        };
        curBlock.addLast(instr);
        return instr;
    }

    private Value visitUnaryExp(UnaryExpAST unaryExp) {
        Value nVal = visitExp(unaryExp.next());
        return switch (unaryExp.type()) {
            case F2I -> typeConversion(nVal, BasicType.I32);
            case I2F -> typeConversion(nVal, BasicType.FLOAT);
            case NEG -> {
                Instr instr;
                if (nVal.getType() == BasicType.FLOAT) {
                    instr = new FnegInstr(nVal);
                } else {
                    nVal = typeConversion(nVal, BasicType.I32);
                    instr = new BinaryInstr(BinaryInstr.Op.SUB, new I32Constant(0), nVal);
                }
                curBlock.addLast(instr);
                yield instr;
            }
            case L_NOT -> {
                if (nVal.getType() == BasicType.I1) {
                    Instr xorInstr = new BinaryInstr(BinaryInstr.Op.XOR, nVal, I1Constant.TRUE);
                    curBlock.addLast(xorInstr);
                    yield xorInstr;
                }
                Instr cmpInstr = new CmpInstr(CmpInstr.Op.EQ, nVal, new I32Constant(0));
                curBlock.addLast(cmpInstr);
                yield cmpInstr;
            }
        };
    }

    // TODO too much duplicated code, extract the commons
    private Value visitVarExp(VarExpAST varExp) {
        if (varExp.symbol() instanceof GlobalSymbol symbol) {
            if (symbol.isConst()) {
                Value ptr = module.getGlobal(symbol.getName());
                List<ExpAST> dimensions = varExp.dimensions();
                for (ExpAST dimension : dimensions) {
                    Value index = visitExp(dimension);
                    Instr ptrInstr = new GetElementPtrInstr(ptr, new I32Constant(0), index);
                    curBlock.addLast(ptrInstr);
                    ptr = ptrInstr;
                }
                Instr instr = new LoadInstr(ptr);
                curBlock.addLast(instr);
                return instr;
            } else {
                Value ptr = module.getGlobal(symbol.getName());
                List<ExpAST> dimensions = varExp.dimensions();
                for (ExpAST dimension : dimensions) {
                    Value index = visitExp(dimension);
                    Instr ptrInstr = new GetElementPtrInstr(ptr, new I32Constant(0), index);
                    curBlock.addLast(ptrInstr);
                    ptr = ptrInstr;
                }
                Instr instr;
                if (varExp.dimensions().size() == symbol.getDimensions().size()) {
                    instr = new LoadInstr(ptr);
                } else {
                    instr = new GetElementPtrInstr(ptr, new I32Constant(0), new I32Constant(0));
                }
                curBlock.addLast(instr);
                return instr;
            }
        }
        if (varExp.symbol() instanceof ParamSymbol paramSymbol) {
            Value ptr = symbolTable.getValue(paramSymbol.getName());
            Instr instr = new LoadInstr(ptr);
            curBlock.addLast(instr);
            ptr = instr;
            List<ExpAST> dimensions = varExp.dimensions();
            if (!dimensions.isEmpty()) {
                Value index = visitExp(dimensions.get(0));
                Instr ptrInstr = new GetElementPtrInstr(ptr, index);
                curBlock.addLast(ptrInstr);
                ptr = ptrInstr;
                for (int i = 1; i < dimensions.size(); i++) {
                    ExpAST dimension = dimensions.get(i);
                    index = visitExp(dimension);
                    ptrInstr = new GetElementPtrInstr(ptr, new I32Constant(0), index);
                    curBlock.addLast(ptrInstr);
                    ptr = ptrInstr;
                }
                instr = new LoadInstr(ptr);
                curBlock.addLast(instr);
                ptr = instr;
            }
            return ptr;
        }
        if (varExp.symbol() instanceof LocalSymbol localSymbol) {
            Value ptr = symbolTable.getValue(localSymbol.getName());
            List<ExpAST> dimensions = varExp.dimensions();
            for (ExpAST dimension : dimensions) {
                Value index = visitExp(dimension);
                Instr ptrInstr = new GetElementPtrInstr(ptr, new I32Constant(0), index);
                curBlock.addLast(ptrInstr);
                ptr = ptrInstr;
            }
            Instr instr;
            if (varExp.dimensions().size() == localSymbol.getDimensions().size()) {
                instr = new LoadInstr(ptr);
            } else {
                instr = new GetElementPtrInstr(ptr, new I32Constant(0), new I32Constant(0));
            }
            curBlock.addLast(instr);
            return instr;
        }
        throw new RuntimeException("Unprocessed VarExpAST: " + varExp);
    }

    private Value visitFuncCallExp(FuncCallExpAST funcCallExp) {
        List<Value> params = new ArrayList<>();
        for (int i = 0; i < funcCallExp.params().size(); i++) {
            ExpAST exp = funcCallExp.params().get(i);
            Value param = visitExp(exp);
            if (param.getType() == BasicType.I32 || param.getType() == BasicType.FLOAT) {
                Type targetType = funcCallExp.func().getParams().get(i).isFloat() ? BasicType.FLOAT : BasicType.I32;
                param = typeConversion(param, targetType);
            }
            params.add(param);
        }
        Type retType = BasicType.VOID;
        if (funcCallExp.func().hasRet()) {
            retType = funcCallExp.func().isFloat() ? BasicType.FLOAT : BasicType.I32;
        }
        CallInstr callInstr = new CallInstr(retType, funcCallExp.func().getName(), params);
        curBlock.addLast(callInstr);
        return callInstr;
    }

    private Value visitIntLitExp(IntLitExpAST intLitExp) {
        return new I32Constant(intLitExp.value());
    }

    private Value visitFloatLitExp(FloatLitExpAST floatLitExp) {
        return new FloatConstant(floatLitExp.value());
    }

    private Value typeConversion(Value value, Type targetType) {
        Type sourceType = value.getType();
        if (sourceType == targetType) {
            return value;
        }
        if (sourceType == BasicType.I1) {
            if (targetType == BasicType.I32) {
                Instr newValue = new ZextInstr(BasicType.I32, value);
                curBlock.addLast(newValue);
                return newValue;
            }
        }
        if (sourceType == BasicType.I32 && targetType == BasicType.FLOAT) {
            Instr newValue = new SitofpInstr(value);
            curBlock.addLast(newValue);
            return newValue;
        }
        if (sourceType == BasicType.FLOAT && targetType == BasicType.I32) {
            Instr newValue = new FptosiInstr(value);
            curBlock.addLast(newValue);
            return newValue;
        }
        throw new RuntimeException("Unsupported type conversion: " + sourceType + " -> " + targetType);
    }

    public Module getModule() {
        checkIfIsProcessed();
        return module;
    }
}
