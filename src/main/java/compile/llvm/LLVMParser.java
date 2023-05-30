package compile.llvm;

import compile.llvm.ir.Module;
import compile.llvm.ir.*;
import compile.llvm.ir.constant.*;
import compile.llvm.ir.instr.*;
import compile.llvm.ir.type.ArrayType;
import compile.llvm.ir.type.BasicType;
import compile.llvm.ir.type.PointerType;
import compile.llvm.ir.type.Type;
import compile.symbol.*;
import compile.syntax.ast.*;
import functional.TriFunction;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

// TODO reconstruct the class
public class LLVMParser {
    private static class TypeConversionCase {
        private final Type sourceType, targetType;

        public TypeConversionCase(Type sourceType, Type targetType) {
            this.sourceType = sourceType;
            this.targetType = targetType;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof TypeConversionCase typeConversionCase) {
                if (sourceType == null || typeConversionCase.sourceType == null) {
                    return targetType.equals(typeConversionCase.targetType);
                }
                return sourceType.equals(typeConversionCase.sourceType) && targetType.equals(typeConversionCase.targetType);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(targetType);
        }
    }

    private static final Map<CmpExpAST.Type, CmpInstr.Op> CMP_OPS = Map.of(CmpExpAST.Type.EQ, CmpInstr.Op.EQ, CmpExpAST.Type.NE, CmpInstr.Op.NE, CmpExpAST.Type.GE, CmpInstr.Op.GE, CmpExpAST.Type.GT, CmpInstr.Op.GT, CmpExpAST.Type.LE, CmpInstr.Op.LE, CmpExpAST.Type.LT, CmpInstr.Op.LT);
    private static final Map<Class<? extends CompUnitAST>, BiConsumer<LLVMParser, CompUnitAST>> VISIT_COMP_UNIT_ACTIONS;
    private static final Map<Class<? extends StmtAST>, BiConsumer<LLVMParser, StmtAST>> VISIT_STMT_ACTIONS;
    private static final Map<Class<? extends ExpAST>, BiFunction<LLVMParser, ExpAST, Value>> VISIT_EXP_ACTIONS;
    private static final Map<Class<? extends ExpAST>, BiConsumer<LLVMParser, ExpAST>> VISIT_COND_ACTIONS;
    private static final Map<Class<? extends DataSymbol>, TriFunction<LLVMParser, DataSymbol, List<ExpAST>, Value>> VISIT_VAR_EXP_ACTIONS;
    private static final Map<TypeConversionCase, BiFunction<LLVMParser, Value, Value>> TYPE_CONVERSION_CASES;

    static {
        Map<Class<? extends CompUnitAST>, BiConsumer<LLVMParser, CompUnitAST>> visitCompUnitActions = new HashMap<>();
        visitCompUnitActions.put(ConstDefAST.class, (parser, compUnit) -> parser.visitConstDef((ConstDefAST) compUnit));
        visitCompUnitActions.put(GlobalDefAST.class, (parser, compUnit) -> parser.visitGlobalDef((GlobalDefAST) compUnit));
        visitCompUnitActions.put(FuncDefAST.class, (parser, compUnit) -> parser.visitFuncDef((FuncDefAST) compUnit));
        VISIT_COMP_UNIT_ACTIONS = visitCompUnitActions;
        Map<Class<? extends StmtAST>, BiConsumer<LLVMParser, StmtAST>> visitStmtActions = new HashMap<>();
        visitStmtActions.put(ConstDefAST.class, (parser, stmt) -> parser.visitConstDef((ConstDefAST) stmt));
        visitStmtActions.put(LocalDefAST.class, (parser, stmt) -> parser.visitLocalDef((LocalDefAST) stmt));
        visitStmtActions.put(BlockStmtAST.class, (parser, stmt) -> {
            parser.symbolTable.in();
            parser.visitBlockStmt((BlockStmtAST) stmt);
            parser.symbolTable.out();
        });
        visitStmtActions.put(IfStmtAST.class, (parser, stmt) -> parser.visitIfStmt((IfStmtAST) stmt));
        visitStmtActions.put(WhileStmtAST.class, (parser, stmt) -> parser.visitWhileStmt((WhileStmtAST) stmt));
        visitStmtActions.put(ContinueStmtAST.class, (parser, stmt) -> parser.visitContinueStmt());
        visitStmtActions.put(BreakStmtAST.class, (parser, stmt) -> parser.visitBreakStmt());
        visitStmtActions.put(BlankStmtAST.class, (parser, stmt) -> {
        });
        visitStmtActions.put(AssignStmtAST.class, (parser, stmt) -> parser.visitAssignStmt((AssignStmtAST) stmt));
        visitStmtActions.put(ExpStmtAST.class, (parser, stmt) -> parser.visitExpStmt((ExpStmtAST) stmt));
        visitStmtActions.put(RetStmtAST.class, (parser, stmt) -> parser.visitRetStmt((RetStmtAST) stmt));
        VISIT_STMT_ACTIONS = visitStmtActions;
        Map<Class<? extends ExpAST>, BiFunction<LLVMParser, ExpAST, Value>> visitExpActions = new HashMap<>();
        visitExpActions.put(BinaryExpAST.class, (parser, exp) -> parser.visitBinaryExp((BinaryExpAST) exp));
        visitExpActions.put(CmpExpAST.class, (parser, exp) -> parser.visitCmpExp((CmpExpAST) exp));
        visitExpActions.put(LNotExpAST.class, (parser, exp) -> parser.visitLNotExp((LNotExpAST) exp));
        visitExpActions.put(UnaryExpAST.class, (parser, exp) -> parser.visitUnaryExp((UnaryExpAST) exp));
        visitExpActions.put(VarExpAST.class, (parser, exp) -> parser.visitVarExp((VarExpAST) exp));
        visitExpActions.put(FuncCallExpAST.class, (parser, exp) -> parser.visitFuncCallExp((FuncCallExpAST) exp));
        visitExpActions.put(IntLitExpAST.class, (parser, exp) -> parser.visitIntLitExp((IntLitExpAST) exp));
        visitExpActions.put(FloatLitExpAST.class, (parser, exp) -> parser.visitFloatLitExp((FloatLitExpAST) exp));
        VISIT_EXP_ACTIONS = visitExpActions;
        Map<Class<? extends ExpAST>, BiConsumer<LLVMParser, ExpAST>> visitCondActions = new HashMap<>();
        visitCondActions.put(LOrExpAST.class, (parser, exp) -> parser.visitLOrCond((LOrExpAST) exp));
        visitCondActions.put(LAndExpAST.class, (parser, exp) -> parser.visitLAndCond((LAndExpAST) exp));
        visitCondActions.put(CmpExpAST.class, (parser, exp) -> parser.visitCmpCond((CmpExpAST) exp));
        visitCondActions.put(LNotExpAST.class, (parser, exp) -> parser.visitLNotCond((LNotExpAST) exp));
        VISIT_COND_ACTIONS = visitCondActions;
        Map<Class<? extends DataSymbol>, TriFunction<LLVMParser, DataSymbol, List<ExpAST>, Value>> visitVarExpActions = new HashMap<>();
        visitVarExpActions.put(GlobalSymbol.class, (parser, symbol, dimensions) -> parser.visitVarExpForGlobal((GlobalSymbol) symbol, dimensions));
        visitVarExpActions.put(LocalSymbol.class, (parser, symbol, dimensions) -> parser.visitVarExpForLocal((LocalSymbol) symbol, dimensions));
        visitVarExpActions.put(ParamSymbol.class, (parser, symbol, dimensions) -> parser.visitVarExpForParam((ParamSymbol) symbol, dimensions));
        VISIT_VAR_EXP_ACTIONS = visitVarExpActions;
        Map<TypeConversionCase, BiFunction<LLVMParser, Value, Value>> typeConversionCases = new HashMap<>();
        typeConversionCases.put(new TypeConversionCase(BasicType.I1, BasicType.I32), (parser, value) -> {
            Instr newValue = new ZextInstr(BasicType.I32, value);
            parser.curBlock.addLast(newValue);
            return newValue;
        });
        typeConversionCases.put(new TypeConversionCase(BasicType.I1, BasicType.FLOAT), (parser, value) -> {
            Instr newValue1 = new ZextInstr(BasicType.I32, value);
            Instr newValue2 = new SitofpInstr(newValue1);
            parser.curBlock.addLast(newValue1);
            parser.curBlock.addLast(newValue2);
            return newValue2;
        });
        typeConversionCases.put(new TypeConversionCase(BasicType.I32, BasicType.FLOAT), (parser, value) -> {
            Instr newValue = new SitofpInstr(value);
            parser.curBlock.addLast(newValue);
            return newValue;
        });
        typeConversionCases.put(new TypeConversionCase(BasicType.FLOAT, BasicType.I32), (parser, value) -> {
            Instr newValue = new FptosiInstr(value);
            parser.curBlock.addLast(newValue);
            return newValue;
        });
        typeConversionCases.put(new TypeConversionCase(null, new PointerType(BasicType.I8)), (parser, value) -> {
            Instr newValue = new BitCastInstr(value, new PointerType(BasicType.I8));
            parser.curBlock.addLast(newValue);
            return newValue;
        });
        TYPE_CONVERSION_CASES = typeConversionCases;
    }

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
        func.addParam(new Param(new PointerType(BasicType.I8), "s"));
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
        VISIT_COMP_UNIT_ACTIONS.get(compUnit.getClass()).accept(this, compUnit);
    }

    private void visitConstDef(ConstDefAST constDef) {
        GlobalSymbol constSymbol = constDef.symbol();
        String name = constSymbol.getName();
        Global global;
        if (constSymbol.getType() instanceof BasicType basicType) {
            global = new Global(name, basicType == BasicType.I32 ? new I32Constant(constSymbol.getValue().intValue()) : new FloatConstant(constSymbol.getValue().floatValue()));
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
        if (globalSymbol.getType() instanceof BasicType basicType) {
            global = new Global(name, basicType == BasicType.I32 ? new I32Constant(globalSymbol.getValue().intValue()) : new FloatConstant(globalSymbol.getValue().floatValue()));
        } else {
            global = parseGlobalSymbol(globalSymbol);
        }
        module.addGlobal(global);
        symbolTable.putFirst(name, global);
    }

    private void visitLocalDef(LocalDefAST localDef) {
        LocalSymbol localSymbol = localDef.symbol();
        AllocInstr allocInstr = new AllocInstr(localSymbol.getType());
        allocBlock.addLast(allocInstr);
        symbolTable.putLast(localSymbol.getName(), allocInstr);
    }

    private Global parseGlobalSymbol(GlobalSymbol symbol) {
        BasicType basicType = symbol.getType().getRootBase();
        Map<Integer, Constant> values = symbol.getValues().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> basicType == BasicType.I32 ? new I32Constant(entry.getValue().intValue()) : new FloatConstant(entry.getValue().floatValue())));
        Type type = symbol.getType().getRootBase();
        List<Integer> dimensions = ((ArrayType) symbol.getType()).dimensions();
        ListIterator<Integer> iterator = dimensions.listIterator(dimensions.size());
        while (iterator.hasPrevious()) {
            int dimension = iterator.previous();
            Map<Integer, Constant> newValues = new HashMap<>();
            Set<Integer> indexes = values.keySet().stream().map(index -> index / dimension).collect(Collectors.toSet());
            for (int index : indexes) {
                Map<Integer, Value> valueMap = new HashMap<>();
                for (int i = 0; i < dimension; i++) {
                    Type finalType = type;
                    valueMap.put(i, values.computeIfAbsent(i + index * dimension, j -> new ZeroConstant(finalType)));
                }
                newValues.put(index, new ArrConstant(type, dimension, valueMap));
            }
            values = newValues;
            type = new ArrayType(type, dimension);
        }
        return new Global(symbol.getName(), values.getOrDefault(0, new ZeroConstant(type)));
    }

    private void visitFuncDef(FuncDefAST funcDef) {
        symbolTable.in();
        FuncSymbol funcSymbol = funcDef.decl();
        curFunc = new Function(funcSymbol.getType(), funcSymbol.getName());
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
        Type type = paramSymbol.getType();
        Param param = new Param(type, paramSymbol.getName());
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
        VISIT_STMT_ACTIONS.get(stmt.getClass()).accept(this, stmt);
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
        if (VISIT_COND_ACTIONS.containsKey(cond.getClass())) {
            VISIT_COND_ACTIONS.get(cond.getClass()).accept(this, cond);
            return;
        }
        Value cVal = visitExp(cond);
        Instr cmpInstr = cVal.getType() == BasicType.I32 ? new ICmpInstr(CmpInstr.Op.NE, cVal, new I32Constant(0)) : new FCmpInstr(CmpInstr.Op.NE, cVal, new FloatConstant(0));
        curBlock.addLast(cmpInstr);
        Instr branchInstr = new BranchInstr(cmpInstr, trueBlock, falseBlock);
        curBlock.addLast(branchInstr);
    }

    private void visitLOrCond(LOrExpAST lOrExp) {
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

    private void visitLAndCond(LAndExpAST lAndCond) {
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

    private void visitLNotCond(LNotExpAST lNotCond) {
        BasicBlock trueBlock = this.trueBlock;
        BasicBlock falseBlock = this.falseBlock;
        this.trueBlock = falseBlock;
        this.falseBlock = trueBlock;
        visitCond(lNotCond.next());
    }

    private void visitCmpCond(CmpExpAST cmpCond) {
        Value lVal = visitExp(cmpCond.left());
        Value rVal = visitExp(cmpCond.right());
        Type targetType = lVal.getType() == BasicType.FLOAT || rVal.getType() == BasicType.FLOAT ? BasicType.FLOAT : BasicType.I32;
        lVal = typeConversion(lVal, targetType);
        rVal = typeConversion(rVal, targetType);
        Instr cmpInstr = targetType == BasicType.I32 ? new ICmpInstr(CMP_OPS.get(cmpCond.type()), lVal, rVal) : new FCmpInstr(CMP_OPS.get(cmpCond.type()), lVal, rVal);
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
            List<ExpAST> dimensions = lVal.dimensions();
            boolean isFirst = true;
            for (ExpAST dimension : dimensions) {
                Value index = visitExp(dimension);
                Instr ptrInstr;
                if (isFirst) {
                    ptrInstr = new GetElementPtrInstr(ptr, index);
                } else {
                    ptrInstr = new GetElementPtrInstr(ptr, new I32Constant(0), index);
                }
                isFirst = false;
                curBlock.addLast(ptrInstr);
                ptr = ptrInstr;
            }
            return ptr;
        }
        List<ExpAST> dimensions = lVal.dimensions();
        for (ExpAST dimension : dimensions) {
            Value index = visitExp(dimension);
            Instr ptrInstr = new GetElementPtrInstr(ptr, new I32Constant(0), index);
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
        return VISIT_EXP_ACTIONS.get(exp.getClass()).apply(this, exp);
    }

    private Value visitCmpExp(CmpExpAST binaryExp) {
        Value lVal = visitExp(binaryExp.left());
        Value rVal = visitExp(binaryExp.right());
        Type targetType = automaticTypePromotion(lVal.getType(), rVal.getType());
        lVal = typeConversion(lVal, targetType);
        rVal = typeConversion(rVal, targetType);
        Instr instr = targetType == BasicType.I32 ? new ICmpInstr(CMP_OPS.get(binaryExp.type()), lVal, rVal) : new FCmpInstr(CMP_OPS.get(binaryExp.type()), lVal, rVal);
        curBlock.addLast(instr);
        return instr;
    }

    private Value visitBinaryExp(BinaryExpAST binaryExp) {
        Value lVal = visitExp(binaryExp.left());
        Value rVal = visitExp(binaryExp.right());
        Type targetType = automaticTypePromotion(lVal.getType(), rVal.getType());
        lVal = typeConversion(lVal, targetType);
        rVal = typeConversion(rVal, targetType);
        Instr instr = new BinaryInstr(switch (binaryExp.type()) {
            case ADD -> targetType == BasicType.I32 ? BinaryInstr.Op.ADD : BinaryInstr.Op.FADD;
            case SUB -> targetType == BasicType.I32 ? BinaryInstr.Op.SUB : BinaryInstr.Op.FSUB;
            case DIV -> targetType == BasicType.I32 ? BinaryInstr.Op.SDIV : BinaryInstr.Op.FDIV;
            case MUL -> targetType == BasicType.I32 ? BinaryInstr.Op.MUL : BinaryInstr.Op.FMUL;
            case MOD -> BinaryInstr.Op.SREM;
        }, lVal, rVal);
        curBlock.addLast(instr);
        return instr;
    }

    private Value visitLNotExp(LNotExpAST lNotExp) {
        Value nVal = visitExp(lNotExp.next());
        if (nVal.getType() == BasicType.I1) {
            Instr xorInstr = new BinaryInstr(BinaryInstr.Op.XOR, nVal, I1Constant.TRUE);
            curBlock.addLast(xorInstr);
            return xorInstr;
        }
        Instr cmpInstr = new ICmpInstr(CmpInstr.Op.EQ, nVal, new I32Constant(0));
        curBlock.addLast(cmpInstr);
        return cmpInstr;
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
        };
    }

    private Value visitVarExp(VarExpAST varExp) {
        List<ExpAST> dimensions = varExp.dimensions();
        DataSymbol symbol = varExp.symbol();
        return VISIT_VAR_EXP_ACTIONS.get(symbol.getClass()).apply(this, symbol, dimensions);
    }

    private Value visitVarExpForGlobal(GlobalSymbol symbol, List<ExpAST> dimensions) {
        Value ptr = module.getGlobal(symbol.getName());
        for (ExpAST dimension : dimensions) {
            Value index = visitExp(dimension);
            Instr ptrInstr = new GetElementPtrInstr(ptr, new I32Constant(0), index);
            curBlock.addLast(ptrInstr);
            ptr = ptrInstr;
        }
        Instr instr;
        if (symbol.getType() instanceof BasicType || dimensions.size() == ((ArrayType) symbol.getType()).dimensions().size()) {
            instr = new LoadInstr(ptr);
        } else {
            instr = new GetElementPtrInstr(ptr, new I32Constant(0), new I32Constant(0));
        }
        curBlock.addLast(instr);
        return instr;
    }

    private Value visitVarExpForParam(ParamSymbol symbol, List<ExpAST> dimensions) {
        Value ptr = symbolTable.getValue(symbol.getName());
        Instr instr = new LoadInstr(ptr);
        curBlock.addLast(instr);
        ptr = instr;
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

    private Value visitVarExpForLocal(LocalSymbol symbol, List<ExpAST> dimensions) {
        Value ptr = symbolTable.getValue(symbol.getName());
        for (ExpAST dimension : dimensions) {
            Value index = visitExp(dimension);
            Instr ptrInstr = new GetElementPtrInstr(ptr, new I32Constant(0), index);
            curBlock.addLast(ptrInstr);
            ptr = ptrInstr;
        }
        Instr instr;
        if (symbol.getType() instanceof BasicType || dimensions.size() == ((ArrayType) symbol.getType()).dimensions().size()) {
            instr = new LoadInstr(ptr);
        } else {
            instr = new GetElementPtrInstr(ptr, new I32Constant(0), new I32Constant(0));
        }
        curBlock.addLast(instr);
        return instr;
    }

    private Value visitFuncCallExp(FuncCallExpAST funcCallExp) {
        List<Value> params = new ArrayList<>();
        for (int i = 0; i < funcCallExp.params().size(); i++) {
            ExpAST exp = funcCallExp.params().get(i);
            Value param = visitExp(exp);
            Type targetType = funcCallExp.func().getParams().get(i).getType();
            param = typeConversion(param, targetType);
            params.add(param);
        }
        CallInstr callInstr = new CallInstr(funcCallExp.func().getType(), funcCallExp.func().getName(), params);
        curBlock.addLast(callInstr);
        return callInstr;
    }

    private Value visitIntLitExp(IntLitExpAST intLitExp) {
        return new I32Constant(intLitExp.value());
    }

    private Value visitFloatLitExp(FloatLitExpAST floatLitExp) {
        return new FloatConstant(floatLitExp.value());
    }

    private static Type automaticTypePromotion(Type type1, Type type2) {
        return type1 == BasicType.FLOAT || type2 == BasicType.FLOAT ? BasicType.FLOAT : BasicType.I32;
    }

    private Value typeConversion(Value value, Type targetType) {
        Type sourceType = value.getType();
        if (sourceType.equals(targetType)) {
            return value;
        }
        return TYPE_CONVERSION_CASES.get(new TypeConversionCase(sourceType, targetType)).apply(this, value);
    }

    public Module getModule() {
        checkIfIsProcessed();
        return module;
    }
}
