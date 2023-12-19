package compile;

import compile.sysy.SysYBaseVisitor;
import compile.sysy.SysYParser;
import compile.vir.Argument;
import compile.vir.Block;
import compile.vir.GlobalVariable;
import compile.vir.VirtualFunction;
import compile.vir.contant.Constant;
import compile.vir.contant.ConstantArray;
import compile.vir.contant.ConstantNumber;
import compile.vir.contant.ConstantZero;
import compile.vir.ir.*;
import compile.vir.type.ArrayType;
import compile.vir.type.BasicType;
import compile.vir.type.PointerType;
import compile.vir.type.Type;
import compile.vir.value.Value;

import java.util.*;
import java.util.stream.Collectors;

public class ASTVisitor extends SysYBaseVisitor<Object> {
    private final SysYParser.RootContext rootAST;
    private final Set<GlobalVariable> globals = new HashSet<>();
    private final Map<String, VirtualFunction> funcs = new HashMap<>();
    private final SymbolTable symbolTable = new SymbolTable();
    private final Map<Argument, AllocaVIR> argToAllocaMap = new HashMap<>();
    private final Deque<Block> continueStack = new ArrayDeque<>();
    private final Deque<Block> breakStack = new ArrayDeque<>();
    private boolean isProcessed = false;
    private VirtualFunction curFunc;
    private Value curRetVal;
    private boolean isConst;
    private Type curType;
    private Block retBlock, curBlock, trueBlock, falseBlock;
    private List<AllocaVIR> allocaVIRs;

    public ASTVisitor(SysYParser.RootContext rootAST) {
        this.rootAST = rootAST;
        symbolTable.in();
        initBuiltInFuncs();
        initSyscalls();
    }

    private void checkIfIsProcessed() {
        if (isProcessed)
            return;
        isProcessed = true;
        visitRoot(rootAST);
        formatVIRs();
    }

    public Map<String, VirtualFunction> getFuncs() {
        checkIfIsProcessed();
        return funcs;
    }

    public Set<GlobalVariable> getGlobals() {
        checkIfIsProcessed();
        return globals;
    }

    private void formatVIRs() {
        for (VirtualFunction func : funcs.values()) {
            if (func.isDeclare())
                continue;
            List<Block> blocks = func.getBlocks();
            for (int i = 0; i < blocks.size(); i++) {
                Block block = blocks.get(i);
                if (block.isEmpty() || !(block.getLast() instanceof RetVIR || block.getLast() instanceof BranchVIR)) {
                    block.add(new BranchVIR(blocks.get(i + 1)));
                }
            }
        }
    }

    private void initBuiltInFuncs() {
        funcs.put("getint", symbolTable.makeFunc(BasicType.I32, "getint"));
        funcs.put("getch", symbolTable.makeFunc(BasicType.I32, "getch"));
        funcs.put("getarray", symbolTable.makeFunc(BasicType.I32, "getarray").addArg(new Argument(new PointerType(BasicType.I32), "a")));
        funcs.put("getfloat", symbolTable.makeFunc(BasicType.FLOAT, "getfloat"));
        funcs.put("getfarray", symbolTable.makeFunc(BasicType.I32, "getfarray").addArg(new Argument(new PointerType(BasicType.FLOAT), "a")));
        funcs.put("putint", symbolTable.makeFunc(BasicType.VOID, "putint").addArg(new Argument(BasicType.I32, "a")));
        funcs.put("putch", symbolTable.makeFunc(BasicType.VOID, "putch").addArg(new Argument(BasicType.I32, "a")));
        funcs.put("putarray", symbolTable.makeFunc(BasicType.VOID, "putarray").addArg(new Argument(BasicType.I32, "n")).addArg(new Argument(new PointerType(BasicType.I32), "a")));
        funcs.put("putfloat", symbolTable.makeFunc(BasicType.VOID, "putfloat").addArg(new Argument(BasicType.FLOAT, "a")));
        funcs.put("putfarray", symbolTable.makeFunc(BasicType.VOID, "putfarray").addArg(new Argument(BasicType.I32, "n")).addArg(new Argument(new PointerType(BasicType.FLOAT), "a")));
        funcs.put("_sysy_starttime", symbolTable.makeFunc(BasicType.VOID, "_sysy_starttime").addArg(new Argument(BasicType.I32, "lineno")));
        funcs.put("_sysy_stoptime", symbolTable.makeFunc(BasicType.VOID, "_sysy_stoptime").addArg(new Argument(BasicType.I32, "lineno")));
    }

    private void initSyscalls() {
        funcs.put("memset", symbolTable.makeFunc(BasicType.VOID, "memset").addArg(new Argument(new PointerType(BasicType.I32), "addr")).addArg(new Argument(BasicType.I32, "value")).addArg(new Argument(BasicType.I32, "size")));
    }

    @Override
    public Object visitRoot(SysYParser.RootContext ctx) {

        return super.visitRoot(ctx);
    }

    @Override
    public Type visitType(SysYParser.TypeContext ctx) {
        curType = switch (ctx.getText()) {
            case "int" -> BasicType.I32;
            case "float" -> BasicType.FLOAT;
            case "void" -> BasicType.VOID;
            default -> throw new IllegalStateException("Unexpected value: " + ctx.getText());
        };
        return curType;
    }

    @Override
    public List<Integer> visitDimensions(SysYParser.DimensionsContext ctx) {
        List<Integer> dimensions = new ArrayList<>();
        for (SysYParser.AdditiveExpContext exp : ctx.additiveExp()) {
            dimensions.add(((ConstantNumber) visitAdditiveExp(exp)).intValue());
        }
        return dimensions;
    }

    @Override
    public Object visitVarDecl(SysYParser.VarDeclContext ctx) {
        isConst = ctx.CONST() != null;
        return super.visitVarDecl(ctx);
    }

    @Override
    public Object visitScalarVarDef(SysYParser.ScalarVarDefContext ctx) {
        String name = ctx.Ident().getSymbol().getText();
        if (isConst || symbolTable.size() == 1) {
            Number value = 0;
            if (ctx.additiveExp() != null)
                value = ((ConstantNumber) visitAdditiveExp(ctx.additiveExp())).getValue();
            globals.add(symbolTable.makeGlobal(isConst, curType, name, value));
            return null;
        }
        AllocaVIR allocaVIR = symbolTable.makeLocal(curType, name);
        allocaVIRs.add(allocaVIR);
        SysYParser.AdditiveExpContext valueExp = ctx.additiveExp();
        if (valueExp != null) {
            Value value = visitAdditiveExp(valueExp);
            value = typeConversion(value, curType);
            curBlock.add(new StoreVIR(value, allocaVIR));
        }
        return null;
    }

    @Override
    public Object visitArrayVarDef(SysYParser.ArrayVarDefContext ctx) {
        String name = ctx.Ident().getSymbol().getText();
        List<Integer> dimensions = visitDimensions(ctx.dimensions());
        SysYParser.InitValContext initVal = ctx.initVal();
        if (isConst || symbolTable.size() == 1) {
            SortedMap<Integer, SysYParser.AdditiveExpContext> exps = new TreeMap<>();
            if (initVal != null)
                allocInitVal(dimensions, exps, 0, initVal);
            Type type = dimensions.reversed().stream().reduce(curType, ArrayType::new, (type1, type2) -> type2);
            Map<Integer, Number> values = exps.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, exp -> ((ConstantNumber) visitAdditiveExp(exp.getValue())).getValue()));
            globals.add(symbolTable.makeGlobal(isConst, type, name, values));
            return null;
        }
        AllocaVIR allocaVIR = symbolTable.makeLocal(curType, name, dimensions);
        allocaVIRs.add(allocaVIR);
        if (initVal != null) {
            SortedMap<Integer, SysYParser.AdditiveExpContext> exps = new TreeMap<>();
            allocInitVal(dimensions, exps, 0, initVal);
            BitCastVIR bitCastVIR = new BitCastVIR(new PointerType(BasicType.I32), allocaVIR);
            curBlock.add(bitCastVIR);
            curBlock.add(new CallVIR(symbolTable.getFunc("memset"), List.of(bitCastVIR, new ConstantNumber(0), new ConstantNumber(dimensions.stream().reduce(4, Math::multiplyExact)))));
            for (Map.Entry<Integer, SysYParser.AdditiveExpContext> entry : exps.entrySet()) {
                Value value = typeConversion(visitAdditiveExp(entry.getValue()), curType);
                Value pointer = allocaVIR;
                for (int j = 0; j < dimensions.size(); j++) {
                    int index = entry.getKey() / dimensions.stream().skip(j + 1).reduce(1, Math::multiplyExact) % dimensions.get(j);
                    VIR ir = new GetElementPtrVIR(pointer, new ConstantNumber(0), new ConstantNumber(index));
                    curBlock.add(ir);
                    pointer = ir;
                }
                curBlock.add(new StoreVIR(value, pointer));
            }
        }
        return null;
    }

    @Override
    public Object visitFuncDef(SysYParser.FuncDefContext ctx) {
        argToAllocaMap.clear();
        Type funcType = visitType(ctx.type());
        curFunc = symbolTable.makeFunc(funcType, ctx.Ident().getSymbol().getText());
        symbolTable.in();
        allocaVIRs = new ArrayList<>();
        retBlock = new Block();
        switch (curFunc.getType()) {
            case BasicType.I32, BasicType.FLOAT -> {
                AllocaVIR allocaVIR = new AllocaVIR(curFunc.getType());
                allocaVIRs.add(allocaVIR);
                curRetVal = allocaVIR;
                VIR loadVIR = new LoadVIR(curRetVal);
                retBlock.add(loadVIR);
                retBlock.add(new RetVIR(loadVIR));
            }
            case BasicType.VOID -> {
                curRetVal = null;
                retBlock.add(new RetVIR(null));
            }
            default -> throw new IllegalStateException("Unexpected value: " + curFunc.getType());
        }
        curBlock = new Block();
        curFunc.addBlock(curBlock);
        for (SysYParser.FuncArgContext argCtx : ctx.funcArg()) {
            Argument arg = visitFuncArg(argCtx);
            curFunc.addArg(arg);
            AllocaVIR allocaVIR = symbolTable.makeLocal(arg.getType(), arg.getName());
            allocaVIRs.add(allocaVIR);
            curBlock.add(new StoreVIR(arg, allocaVIR));
            argToAllocaMap.put(arg, allocaVIR);
        }
        visitBlockStmt(ctx.blockStmt());
        curFunc.addBlock(retBlock);
        curFunc.getBlocks().getFirst().addAll(0, allocaVIRs);
        funcs.put(curFunc.getName(), curFunc);
        symbolTable.out();
        return null;
    }

    @Override
    public Argument visitFuncArg(SysYParser.FuncArgContext ctx) {
        visitType(ctx.type());
        Type type = visitType(ctx.type());
        if (!ctx.LB().isEmpty()) {
            for (SysYParser.AdditiveExpContext exp : ctx.additiveExp().reversed())
                type = new ArrayType(type, ((ConstantNumber) visitAdditiveExp(exp)).intValue());
            type = new PointerType(type);
        }
        return symbolTable.makeArg(type, ctx.Ident().getSymbol().getText());
    }

    @Override
    public Object visitBlockStmt(SysYParser.BlockStmtContext ctx) {
        symbolTable.in();
        for (SysYParser.StmtContext stmt : ctx.stmt()) {
            visitStmt(stmt);
            if (stmt.continueStmt() != null || stmt.breakStmt() != null || stmt.retStmt() != null)
                break;
        }
        symbolTable.out();
        return null;
    }

    @Override
    public Object visitAssignStmt(SysYParser.AssignStmtContext ctx) {
        Value pointer = visitLVal(ctx.lVal());
        Value value = visitAdditiveExp(ctx.additiveExp());
        Type type = pointer.getType();
        if (type instanceof BasicType)
            value = typeConversion(value, type);
        else
            value = typeConversion(value, type.baseType());
        curBlock.add(new StoreVIR(value, pointer));
        return null;
    }

    @Override
    public Object visitIfElseStmt(SysYParser.IfElseStmtContext ctx) {
        Block trueBlock = new Block();
        Block falseBlock = new Block();
        Block ifEndBlock = new Block();
        curFunc.insertBlockAfter(curBlock, trueBlock);
        curFunc.insertBlockAfter(trueBlock, falseBlock);
        curFunc.insertBlockAfter(falseBlock, ifEndBlock);
        this.trueBlock = trueBlock;
        this.falseBlock = falseBlock;
        Value value = visitLorExp(ctx.lorExp());
        processValueCond(value);
        curBlock = trueBlock;
        visitStmt(ctx.stmt(0));
        curBlock.add(new BranchVIR(ifEndBlock));
        curBlock = falseBlock;
        visitStmt(ctx.stmt(1));
        curBlock.add(new BranchVIR(ifEndBlock));
        curBlock = ifEndBlock;
        return null;
    }

    @Override
    public Object visitIfStmt(SysYParser.IfStmtContext ctx) {
        Block trueBlock = new Block();
        Block falseBlock = new Block();
        curFunc.insertBlockAfter(curBlock, trueBlock);
        curFunc.insertBlockAfter(trueBlock, falseBlock);
        this.trueBlock = trueBlock;
        this.falseBlock = falseBlock;
        Value value = visitLorExp(ctx.lorExp());
        this.trueBlock = trueBlock;
        this.falseBlock = falseBlock;
        processValueCond(value);
        curBlock = trueBlock;
        visitStmt(ctx.stmt());
        curBlock.add(new BranchVIR(falseBlock));
        curBlock = falseBlock;
        return null;
    }

    @Override
    public Object visitWhileStmt(SysYParser.WhileStmtContext ctx) {
        Block entryBlock = new Block();
        Block loopBlock = new Block();
        Block endBlock = new Block();
        curFunc.insertBlockAfter(curBlock, entryBlock);
        curFunc.insertBlockAfter(entryBlock, loopBlock);
        curFunc.insertBlockAfter(loopBlock, endBlock);
        continueStack.push(entryBlock);
        breakStack.push(endBlock);
        curBlock.add(new BranchVIR(entryBlock));
        curBlock = entryBlock;
        trueBlock = loopBlock;
        falseBlock = endBlock;
        Value value = visitLorExp(ctx.lorExp());
        processValueCond(value);
        curBlock = loopBlock;
        visitStmt(ctx.stmt());
        curBlock.add(new BranchVIR(entryBlock));
        curBlock = endBlock;
        continueStack.pop();
        breakStack.pop();
        return null;
    }

    @Override
    public Object visitBreakStmt(SysYParser.BreakStmtContext ctx) {
        curBlock.add(new BranchVIR(breakStack.peek()));
        return null;
    }

    @Override
    public Object visitContinueStmt(SysYParser.ContinueStmtContext ctx) {
        curBlock.add(new BranchVIR(continueStack.peek()));
        return null;
    }

    @Override
    public Object visitRetStmt(SysYParser.RetStmtContext ctx) {
        if (ctx.additiveExp() == null) {
            curBlock.add(new BranchVIR(retBlock));
            return null;
        }
        Value retVal = visitAdditiveExp(ctx.additiveExp());
        retVal = typeConversion(retVal, curFunc.getType());
        curBlock.add(new StoreVIR(retVal, curRetVal));
        curBlock.add(new BranchVIR(retBlock));
        return null;
    }

    @Override
    public Value visitLVal(SysYParser.LValContext ctx) {
        Value pointer = symbolTable.getData(ctx.Ident().getSymbol().getText());
        boolean isArg = false;
        if (pointer instanceof Argument) {
            isArg = true;
            pointer = argToAllocaMap.get(pointer);
        }
        if (ctx.additiveExp().isEmpty()) {
            return pointer;
        }
        if (pointer.getType() instanceof PointerType pointerType && pointerType.baseType() instanceof PointerType) {
            VIR ir = new LoadVIR(pointer);
            pointer = ir;
            curBlock.add(ir);
        }
        List<Value> dimensions = ctx.additiveExp().stream().map(this::visitAdditiveExp).toList();
        boolean isFirst = true;
        for (Value dimension : dimensions) {
            VIR ir;
            if (isArg && isFirst)
                ir = new GetElementPtrVIR(pointer, dimension);
            else
                ir = new GetElementPtrVIR(pointer, new ConstantNumber(0), dimension);
            curBlock.add(ir);
            pointer = ir;
            isFirst = false;
        }
        return pointer;
    }

    @Override
    public Value visitUnaryExp(SysYParser.UnaryExpContext ctx) {
        return switch (ctx.getChildCount()) {
            case 2 -> {
                Value value = visitUnaryExp(ctx.unaryExp());
                if (value instanceof ConstantNumber number) {
                    yield switch (ctx.getChild(0).getText()) {
                        case "+" -> number;
                        case "-" -> number.neg();
                        case "!" -> number.lNot();
                        default -> throw new IllegalStateException("Unexpected value: " + ctx.getChild(0).getText());
                    };
                }
                yield switch (ctx.getChild(0).getText()) {
                    case "+" -> value;
                    case "-" -> {
                        VIR ir = switch (value.getType()) {
                            case BasicType.I1 -> new SExtVIR(BasicType.I32, value);
                            case BasicType.I32 -> new BinaryVIR(BinaryVIR.Type.SUB, new ConstantNumber(0), value);
                            case BasicType.FLOAT -> new BinaryVIR(BinaryVIR.Type.FSUB, new ConstantNumber(0.0f), value);
                            default -> throw new IllegalStateException("Unexpected value: " + value.getType());
                        };
                        curBlock.add(ir);
                        yield ir;
                    }
                    case "!" -> {
                        VIR ir = switch (value.getType()) {
                            case BasicType.I1 -> new BinaryVIR(BinaryVIR.Type.XOR, value, new ConstantNumber(true));
                            case BasicType.I32 -> new ICmpVIR(ICmpVIR.Cond.EQ, value, new ConstantNumber(0));
                            case BasicType.FLOAT -> new FCmpVIR(FCmpVIR.Cond.OEQ, value, new ConstantNumber(0.0f));
                            default -> throw new IllegalStateException("Unexpected value: " + value.getType());
                        };
                        curBlock.add(ir);
                        yield ir;
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + ctx.getChild(0).getText());
                };
            }
            case 3 -> visitAdditiveExp(ctx.additiveExp());
            default -> (Value) super.visitUnaryExp(ctx);
        };
    }

    @Override
    public Value visitScalarVarExp(SysYParser.ScalarVarExpContext ctx) {
        Value pointer = symbolTable.getData(ctx.Ident().getSymbol().getText());
        if (pointer instanceof Argument)
            pointer = argToAllocaMap.get(pointer);
        Type type = pointer.getType();
        if (pointer instanceof GlobalVariable global) {
            if (global.isConst() && global.isSingle())
                return global.getValue();
            type = new PointerType(type);
        }
        if (type.baseType() instanceof ArrayType arrayType) {
            Value[] indexes = new Value[arrayType.getArraySizes().size()];
            Arrays.fill(indexes, new ConstantNumber(0));
            VIR ir = new GetElementPtrVIR(pointer, indexes);
            curBlock.add(ir);
            return ir;
        }
        VIR ir = new LoadVIR(pointer);
        curBlock.add(ir);
        return ir;
    }

    @Override
    public Value visitArrayVarExp(SysYParser.ArrayVarExpContext ctx) {
        Value pointer = symbolTable.getData(ctx.Ident().getSymbol().getText());
        boolean isFirstArgDim = false;
        if (pointer instanceof Argument) {
            isFirstArgDim = true;
            pointer = argToAllocaMap.get(pointer);
            VIR ir = new LoadVIR(pointer);
            pointer = ir;
            curBlock.add(ir);
        }
        for (SysYParser.AdditiveExpContext dimension : ctx.additiveExp()) {
            Value index = visitAdditiveExp(dimension);
            index = typeConversion(index, BasicType.I32);
            VIR ir = isFirstArgDim ? new GetElementPtrVIR(pointer, index) : new GetElementPtrVIR(pointer, new ConstantNumber(0), index);
            curBlock.add(ir);
            pointer = ir;
            isFirstArgDim = false;
        }
        if (pointer.getType().baseType() instanceof ArrayType)
            return pointer;
        VIR ir = new LoadVIR(pointer);
        curBlock.add(ir);
        return ir;
    }

    @Override
    public Value visitFuncCallExp(SysYParser.FuncCallExpContext ctx) {
        VirtualFunction func = symbolTable.getFunc(ctx.Ident().getSymbol().getText());
        List<Value> params = new ArrayList<>();
        for (int i = 0; i < ctx.additiveExp().size(); i++) {
            SysYParser.AdditiveExpContext exp = ctx.additiveExp().get(i);
            Value param = visitAdditiveExp(exp);
            Type type = func.getArgs().get(i).getType() instanceof BasicType && func.getArgs().get(i).getType() == BasicType.FLOAT ? BasicType.FLOAT : BasicType.I32;
            param = typeConversion(param, type);
            params.add(param);
        }
        VIR newIR = new CallVIR(func, params);
        curBlock.add(newIR);
        return newIR;
    }

    @Override
    public Object visitNumberExp(SysYParser.NumberExpContext ctx) {
        if (ctx.IntConst() != null) {
            return new ConstantNumber(Integer.decode(ctx.IntConst().getSymbol().getText()));
        }
        if (ctx.FloatConst() != null) {
            return new ConstantNumber(Float.parseFloat(ctx.FloatConst().getSymbol().getText()));
        }
        throw new RuntimeException();
    }

    @Override
    public Value visitLorExp(SysYParser.LorExpContext ctx) {
        List<Block> blocks = new ArrayList<>();
        blocks.add(curBlock);
        for (int i = 0; i < ctx.landExp().size() - 1; i++) {
            Block block = new Block();
            curFunc.insertBlockAfter(blocks.getLast(), block);
            blocks.add(block);
        }
        Block trueBlock = this.trueBlock;
        Block falseBlock = this.falseBlock;
        for (int i = 0; i < ctx.landExp().size() - 1; i++) {
            this.curBlock = blocks.get(i);
            this.trueBlock = trueBlock;
            this.falseBlock = blocks.get(i + 1);
            Value value = visitLandExp(ctx.landExp(i));
            processValueCond(value);
        }
        this.curBlock = blocks.getLast();
        this.trueBlock = trueBlock;
        this.falseBlock = falseBlock;
        Value value = visitLandExp(ctx.landExp().getLast());
        processValueCond(value);
        return null;
    }

    @Override
    public Value visitLandExp(SysYParser.LandExpContext ctx) {
        List<Block> blocks = new ArrayList<>();
        blocks.add(curBlock);
        for (int i = 0; i < ctx.equalityExp().size() - 1; i++) {
            Block block = new Block();
            curFunc.insertBlockAfter(blocks.getLast(), block);
            blocks.add(block);
        }
        Block trueBlock = this.trueBlock;
        Block falseBlock = this.falseBlock;
        for (int i = 0; i < ctx.equalityExp().size() - 1; i++) {
            this.curBlock = blocks.get(i);
            this.trueBlock = blocks.get(i + 1);
            this.falseBlock = falseBlock;
            Value value = visitEqualityExp(ctx.equalityExp(i));
            processValueCond(value);
        }
        this.curBlock = blocks.getLast();
        this.trueBlock = trueBlock;
        this.falseBlock = falseBlock;
        Value value = visitEqualityExp(ctx.equalityExp().getLast());
        processValueCond(value);
        return null;
    }

    @Override
    public Value visitEqualityExp(SysYParser.EqualityExpContext ctx) {
        Value iterVal = visitRelationalExp(ctx.relationalExp(0));
        for (int i = 1; i < ctx.relationalExp().size(); i++) {
            Value nextVal = visitRelationalExp(ctx.relationalExp(i));
            Type targetType = automaticTypePromotion(iterVal.getType(), nextVal.getType());
            iterVal = typeConversion(iterVal, targetType);
            nextVal = typeConversion(nextVal, targetType);
            VIR ir = switch (ctx.getChild(2 * i - 1).getText()) {
                case "==" -> switch (targetType) {
                    case BasicType.I32 -> new ICmpVIR(ICmpVIR.Cond.EQ, iterVal, nextVal);
                    case BasicType.FLOAT -> new FCmpVIR(FCmpVIR.Cond.OEQ, iterVal, nextVal);
                    default -> throw new IllegalStateException("Unexpected value: " + targetType);
                };
                case "!=" -> switch (targetType) {
                    case BasicType.I32 -> new ICmpVIR(ICmpVIR.Cond.NE, iterVal, nextVal);
                    case BasicType.FLOAT -> new FCmpVIR(FCmpVIR.Cond.UNE, iterVal, nextVal);
                    default -> throw new IllegalStateException("Unexpected value: " + targetType);
                };
                default -> throw new IllegalStateException("Unexpected value: " + ctx.getChild(2 * i - 1).getText());
            };
            curBlock.add(ir);
            iterVal = ir;
        }
        return iterVal;
    }

    @Override
    public Value visitRelationalExp(SysYParser.RelationalExpContext ctx) {
        Value iterVal = visitAdditiveExp(ctx.additiveExp(0));
        for (int i = 1; i < ctx.additiveExp().size(); i++) {
            Value nextVal = visitAdditiveExp(ctx.additiveExp(i));
            Type targetType = automaticTypePromotion(iterVal.getType(), nextVal.getType());
            iterVal = typeConversion(iterVal, targetType);
            nextVal = typeConversion(nextVal, targetType);
            VIR ir = switch (ctx.getChild(2 * i - 1).getText()) {
                case "<" -> switch (targetType) {
                    case BasicType.I32 -> new ICmpVIR(ICmpVIR.Cond.SLT, iterVal, nextVal);
                    case BasicType.FLOAT -> new FCmpVIR(FCmpVIR.Cond.OLT, iterVal, nextVal);
                    default -> throw new IllegalStateException("Unexpected value: " + targetType);
                };
                case ">" -> switch (targetType) {
                    case BasicType.I32 -> new ICmpVIR(ICmpVIR.Cond.SGT, iterVal, nextVal);
                    case BasicType.FLOAT -> new FCmpVIR(FCmpVIR.Cond.OGT, iterVal, nextVal);
                    default -> throw new IllegalStateException("Unexpected value: " + targetType);
                };
                case "<=" -> switch (targetType) {
                    case BasicType.I32 -> new ICmpVIR(ICmpVIR.Cond.SLE, iterVal, nextVal);
                    case BasicType.FLOAT -> new FCmpVIR(FCmpVIR.Cond.OLE, iterVal, nextVal);
                    default -> throw new IllegalStateException("Unexpected value: " + targetType);
                };
                case ">=" -> switch (targetType) {
                    case BasicType.I32 -> new ICmpVIR(ICmpVIR.Cond.SGE, iterVal, nextVal);
                    case BasicType.FLOAT -> new FCmpVIR(FCmpVIR.Cond.OGE, iterVal, nextVal);
                    default -> throw new IllegalStateException("Unexpected value: " + targetType);
                };
                default -> throw new IllegalStateException("Unexpected value: " + ctx.getChild(1).getText());
            };
            curBlock.add(ir);
            iterVal = ir;
        }
        return iterVal;
    }

    @Override
    public Value visitAdditiveExp(SysYParser.AdditiveExpContext ctx) {
        Value iterVal = visitMultiplicativeExp(ctx.multiplicativeExp(0));
        for (int i = 1; i < ctx.multiplicativeExp().size(); i++) {
            Value nextVal = visitMultiplicativeExp(ctx.multiplicativeExp(i));
            Type targetType = automaticTypePromotion(iterVal.getType(), nextVal.getType());
            iterVal = typeConversion(iterVal, targetType);
            nextVal = typeConversion(nextVal, targetType);
            if (iterVal instanceof ConstantNumber number1 && nextVal instanceof ConstantNumber number2) {
                iterVal = switch (ctx.getChild(i * 2 - 1).getText()) {
                    case "+" -> number1.add(number2);
                    case "-" -> number1.sub(number2);
                    default ->
                            throw new IllegalStateException("Unexpected value: " + ctx.getChild(i * 2 - 1).getText());
                };
                continue;
            }
            VIR ir = new BinaryVIR(switch (ctx.getChild(i * 2 - 1).getText()) {
                case "+" -> switch (targetType) {
                    case BasicType.I32 -> BinaryVIR.Type.ADD;
                    case BasicType.FLOAT -> BinaryVIR.Type.FADD;
                    default -> throw new IllegalStateException("Unexpected value: " + targetType);
                };
                case "-" -> switch (targetType) {
                    case BasicType.I32 -> BinaryVIR.Type.SUB;
                    case BasicType.FLOAT -> BinaryVIR.Type.FSUB;
                    default -> throw new IllegalStateException("Unexpected value: " + targetType);
                };
                default -> throw new IllegalStateException("Unexpected value: " + ctx.getChild(1).getText());
            }, iterVal, nextVal);
            curBlock.add(ir);
            iterVal = ir;
        }
        return iterVal;
    }

    @Override
    public Value visitMultiplicativeExp(SysYParser.MultiplicativeExpContext ctx) {
        Value iterVal = visitUnaryExp(ctx.unaryExp(0));
        for (int i = 1; i < ctx.unaryExp().size(); i++) {
            Value nextVal = visitUnaryExp(ctx.unaryExp(i));
            Type targetType = automaticTypePromotion(iterVal.getType(), nextVal.getType());
            iterVal = typeConversion(iterVal, targetType);
            nextVal = typeConversion(nextVal, targetType);
            if (iterVal instanceof ConstantNumber number1 && nextVal instanceof ConstantNumber number2) {
                iterVal = switch (ctx.getChild(i * 2 - 1).getText()) {
                    case "*" -> number1.mul(number2);
                    case "/" -> number1.div(number2);
                    case "%" -> number1.rem(number2);
                    default ->
                            throw new IllegalStateException("Unexpected value: " + ctx.getChild(i * 2 - 1).getText());
                };
                continue;
            }
            VIR ir = new BinaryVIR(switch (ctx.getChild(i * 2 - 1).getText()) {
                case "*" -> switch (targetType) {
                    case BasicType.I32 -> BinaryVIR.Type.MUL;
                    case BasicType.FLOAT -> BinaryVIR.Type.FMUL;
                    default -> throw new IllegalStateException("Unexpected value: " + targetType);
                };
                case "/" -> switch (targetType) {
                    case BasicType.I32 -> BinaryVIR.Type.SDIV;
                    case BasicType.FLOAT -> BinaryVIR.Type.FDIV;
                    default -> throw new IllegalStateException("Unexpected value: " + targetType);
                };
                case "%" -> BinaryVIR.Type.SREM;
                default -> throw new IllegalStateException("Unexpected value: " + ctx.getChild(1).getText());
            }, iterVal, nextVal);
            curBlock.add(ir);
            iterVal = ir;
        }
        return iterVal;
    }

    private void allocInitVal(List<Integer> dimensions, Map<Integer, SysYParser.AdditiveExpContext> exps, int base, SysYParser.InitValContext src) {
        int offset = 0;
        for (SysYParser.InitValContext exp : src.initVal()) {
            if (exp.additiveExp() == null) {
                int size = dimensions.stream().skip(1).reduce(1, Math::multiplyExact);
                offset = (offset + size - 1) / size * size;
                allocInitVal(dimensions.subList(1, dimensions.size()), exps, base + offset, exp);
                offset += size;
            } else {
                exps.put(base + offset, exp.additiveExp());
                offset++;
            }
        }
    }

    private void processValueCond(Value value) {
        if (value != null) {
            Value cond = switch (value.getType()) {
                case BasicType.I1 -> value;
                case BasicType.I32 -> {
                    VIR ir = new ICmpVIR(ICmpVIR.Cond.NE, value, new ConstantNumber(0));
                    curBlock.add(ir);
                    yield ir;
                }
                case BasicType.FLOAT -> {
                    VIR ir = new FCmpVIR(FCmpVIR.Cond.UNE, value, new ConstantNumber(0.0f));
                    curBlock.add(ir);
                    yield ir;
                }
                default -> throw new IllegalStateException("Unexpected value: " + value.getType());
            };
            curBlock.add(new BranchVIR(cond, this.trueBlock, this.falseBlock));
        }
    }

    private Type automaticTypePromotion(Type type1, Type type2) {
        if (type1 == BasicType.FLOAT || type2 == BasicType.FLOAT)
            return BasicType.FLOAT;
        if (type1 == BasicType.I32 || type2 == BasicType.I32)
            return BasicType.I32;
        return BasicType.I1;
    }

    private Value typeConversion(Value value, Type targetType) {
        if (value.getType() == targetType)
            return value;
        if (value instanceof ConstantNumber number) {
            return switch (targetType) {
                case BasicType.I1 -> new ConstantNumber(number.intValue() != 0);
                case BasicType.I32 -> new ConstantNumber(number.intValue());
                case BasicType.FLOAT -> new ConstantNumber(number.floatValue());
                default -> throw new IllegalStateException("Unexpected value: " + targetType);
            };
        }
        return switch (targetType) {
            case BasicType.I1 -> switch (value.getType()) {
                case BasicType.I32 -> {
                    VIR ir = new ICmpVIR(ICmpVIR.Cond.NE, value, new ConstantNumber(0));
                    curBlock.add(ir);
                    yield ir;
                }
                case BasicType.FLOAT -> {
                    VIR ir = new FCmpVIR(FCmpVIR.Cond.UNE, value, new ConstantNumber(0.0f));
                    curBlock.add(ir);
                    yield ir;
                }
                default -> value;
            };
            case BasicType.I32 -> switch (value.getType()) {
                case BasicType.I1 -> {
                    VIR ir = new ZExtVIR(BasicType.I32, value);
                    curBlock.add(ir);
                    yield ir;
                }
                case BasicType.FLOAT -> {
                    VIR ir = new FPToSIVIR(BasicType.I32, value);
                    curBlock.add(ir);
                    yield ir;
                }
                default -> value;
            };
            case BasicType.FLOAT -> switch (value.getType()) {
                case BasicType.I1 -> {
                    VIR ir = new ZExtVIR(BasicType.I32, value);
                    curBlock.add(ir);
                    ir = new SIToFPVIR(BasicType.FLOAT, ir);
                    curBlock.add(ir);
                    yield ir;
                }
                case BasicType.I32 -> {
                    VIR ir = new SIToFPVIR(BasicType.FLOAT, value);
                    curBlock.add(ir);
                    yield ir;
                }
                default -> value;
            };
            default -> value;
        };
    }

    private static class SymbolTable extends LinkedList<Map<String, Value>> {
        private Value get(String name) {
            for (Map<String, Value> symbols : this)
                if (symbols.containsKey(name))
                    return symbols.get(name);
            throw new RuntimeException("Undefined symbol: " + name);
        }

        public Value getData(String name) {
            return get(name);
        }

        public VirtualFunction getFunc(String name) {
            Value symbol = get(name);
            if (symbol instanceof VirtualFunction function)
                return function;
            throw new RuntimeException("Undefined function symbol: " + name);
        }

        public void in() {
            this.addFirst(new HashMap<>());
        }

        private Constant fuseConst(Type type, SortedMap<Integer, Number> values, int base) {
            if (type instanceof BasicType)
                return new ConstantNumber(values.getOrDefault(base, 0));
            if (values.isEmpty())
                return new ConstantZero(type);
            ArrayType arrayType = (ArrayType) type;
            int size = arrayType.getArraySizes().stream().reduce(1, Math::multiplyExact);
            List<Constant> array = new ArrayList<>();
            for (int i = base; i < base + size; i += size / arrayType.arraySize())
                array.add(fuseConst(arrayType.baseType(), values.subMap(i, i + size / arrayType.arraySize()), i));
            return new ConstantArray(arrayType, array);
        }

        public VirtualFunction makeFunc(Type type, String name) {
            VirtualFunction symbol = new VirtualFunction(type, name);
            this.getLast().put(name, symbol);
            return symbol;
        }

        public GlobalVariable makeGlobal(boolean isConst, Type type, String name, Number value) {
            GlobalVariable symbol = new GlobalVariable(isConst, type, name, new ConstantNumber(switch (type) {
                case BasicType.I32 -> value.intValue();
                case BasicType.FLOAT -> value.floatValue();
                default -> throw new IllegalStateException("Unexpected value: " + type);
            }));
            this.getFirst().put(name, symbol);
            return symbol;
        }

        public GlobalVariable makeGlobal(boolean isConst, Type type, String name, Map<Integer, Number> values) {
            Type rootType = type;
            while (rootType instanceof ArrayType arrayType)
                rootType = arrayType.baseType();
            for (Map.Entry<Integer, Number> entry : values.entrySet()) {
                entry.setValue(switch (rootType) {
                    case BasicType.I32 -> entry.getValue().intValue();
                    case BasicType.FLOAT -> entry.getValue().floatValue();
                    default -> throw new IllegalStateException("Unexpected value: " + rootType);
                });
            }
            GlobalVariable symbol = new GlobalVariable(isConst, type, name, fuseConst(type, new TreeMap<>(values), 0));
            this.getFirst().put(name, symbol);
            return symbol;
        }

        public AllocaVIR makeLocal(Type type, String name) {
            AllocaVIR symbol = new AllocaVIR(type);
            this.getFirst().put(name, symbol);
            return symbol;
        }

        public AllocaVIR makeLocal(Type type, String name, List<Integer> dimensions) {
            for (int i = dimensions.size() - 1; i >= 0; i--)
                type = new ArrayType(type, dimensions.get(i));
            AllocaVIR symbol = new AllocaVIR(type);
            this.getFirst().put(name, symbol);
            return symbol;
        }

        public Argument makeArg(Type type, String name) {
            Argument arg = new Argument(type, name);
            this.getFirst().put(name, arg);
            return arg;
        }

        public void out() {
            this.removeFirst();
        }
    }
}