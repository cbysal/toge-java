package compile;

import common.NumberUtils;
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
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class VIRGenerator extends SysYBaseVisitor<Object> {
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

        private Constant fuseConst(Type type, Map<Integer, Integer> values) {
            if (values.isEmpty()) {
                return new ConstantZero(type);
            }
            List<ArrayType> arrayTypes = new ArrayList<>();
            while (type instanceof ArrayType arrayType) {
                arrayTypes.add(arrayType);
                type = arrayType.getBaseType();
            }
            Collections.reverse(arrayTypes);
            int totalSize = arrayTypes.stream().mapToInt(ArrayType::getArraySize).reduce(1, Math::multiplyExact);
            List<Constant> constants = new ArrayList<>(totalSize / arrayTypes.getFirst().getArraySize());
            for (int i = 0; i < totalSize; i += arrayTypes.getFirst().getArraySize()) {
                int arraySize = arrayTypes.getFirst().getArraySize();
                boolean isEmpty = IntStream.range(i, i + arraySize).noneMatch(values::containsKey);
                if (isEmpty) {
                    constants.add(new ConstantZero(arrayTypes.getFirst()));
                    continue;
                }
                List<Constant> array = new ArrayList<>(arraySize);
                for (int j = 0; j < arraySize; j++) {
                    int index = i + j;
                    ConstantNumber number = switch (type) {
                        case BasicType.I32 -> new ConstantNumber(values.getOrDefault(index, 0));
                        case BasicType.FLOAT -> new ConstantNumber(Float.intBitsToFloat(values.getOrDefault(index, 0)));
                        default -> throw new IllegalStateException("Unexpected value: " + type);
                    };
                    array.add(number);
                }
                constants.add(new ConstantArray(arrayTypes.getFirst(), array));
            }
            for (ArrayType arrayType : arrayTypes.stream().skip(1).toList()) {
                int arraySize = arrayType.getArraySize();
                List<Constant> newConstants = new ArrayList<>(constants.size() / arraySize);
                for (int i = 0; i < constants.size(); i += arraySize) {
                    boolean isEmpty = constants.stream().skip(i).limit(arraySize).allMatch(ConstantZero.class::isInstance);
                    if (isEmpty)
                        newConstants.add(new ConstantZero(arrayType));
                    else
                        newConstants.add(new ConstantArray(arrayType, constants.subList(i, i + arraySize)));
                }
                constants = newConstants;
            }
            return constants.getFirst();
        }

        public GlobalVariable makeConst(Type type, String name, Number value) {
            GlobalVariable symbol = new GlobalVariable(true, type, name, new ConstantNumber(value));
            this.getFirst().put(name, symbol);
            return symbol;
        }

        public GlobalVariable makeConst(Type type, String name, Map<Integer, Integer> values) {

            GlobalVariable symbol = new GlobalVariable(true, type, name, fuseConst(type, values));
            this.getFirst().put(name, symbol);
            return symbol;
        }

        public VirtualFunction makeFunc(Type type, String name) {
            VirtualFunction symbol = new VirtualFunction(type, name);
            this.getLast().put(name, symbol);
            return symbol;
        }

        public GlobalVariable makeGlobal(Type type, String name, Number value) {
            GlobalVariable symbol = new GlobalVariable(false, type, name, new ConstantNumber(value));
            this.getFirst().put(name, symbol);
            return symbol;
        }

        public GlobalVariable makeGlobal(Type type, String name, Map<Integer, Integer> values) {
            GlobalVariable symbol = new GlobalVariable(false, type, name, fuseConst(type, values));
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

    private static final Map<String, UnaryOperator<Number>> UOP_MAP = new HashMap<>() {{
        put("+", NumberUtils::self);
        put("-", NumberUtils::neg);
        put("!", NumberUtils::lnot);
    }};
    private static final Map<String, BinaryOperator<Number>> BIOP_MAP = new HashMap<>() {{
        put("==", (num1, num2) -> NumberUtils.compare(num1, num2) == 0 ? 1 : 0);
        put("!=", (num1, num2) -> NumberUtils.compare(num1, num2) != 0 ? 1 : 0);
        put(">=", (num1, num2) -> NumberUtils.compare(num1, num2) >= 0 ? 1 : 0);
        put(">", (num1, num2) -> NumberUtils.compare(num1, num2) > 0 ? 1 : 0);
        put("<=", (num1, num2) -> NumberUtils.compare(num1, num2) <= 0 ? 1 : 0);
        put("<", (num1, num2) -> NumberUtils.compare(num1, num2) < 0 ? 1 : 0);
        put("+", NumberUtils::add);
        put("-", NumberUtils::sub);
        put("*", NumberUtils::mul);
        put("/", NumberUtils::div);
        put("%", NumberUtils::mod);
        put("&&", NumberUtils::land);
        put("||", NumberUtils::lor);
    }};
    private final SysYParser.RootContext rootAST;
    private final Set<GlobalVariable> globals = new HashSet<>();
    private final Map<String, VirtualFunction> funcs = new HashMap<>();
    private final SymbolTable symbolTable = new SymbolTable();
    private final Map<Argument, AllocaVIR> argToAllocaMap = new HashMap<>();
    private final Deque<Block> continueStack = new ArrayDeque<>();
    private final Deque<Block> breakStack = new ArrayDeque<>();
    private boolean isProcessed = false;
    private VirtualFunction curFunc;
    private boolean isConst;
    private Type curType;
    private Block curBlock, trueBlock, falseBlock;
    private List<AllocaVIR> allocaVIRs;
    private boolean calculatingCond = false;

    public VIRGenerator(SysYParser.RootContext rootAST) {
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
            for (int i = 0; i + 1 < blocks.size(); i++) {
                Block block = blocks.get(i);
                VIR lastIR = block.getLast();
                if (!(lastIR instanceof BranchVIR || lastIR instanceof RetVIR))
                    block.add(new BranchVIR(blocks.get(i + 1)));
            }
            for (int i = 0; i + 1 < blocks.size(); i++) {
                Block block = blocks.get(i);
                for (int j = 0; j + 1 < block.size(); j++) {
                    VIR ir = block.get(j);
                    if ((ir instanceof BranchVIR branchVIR && !branchVIR.conditional()) || ir instanceof RetVIR) {
                        while (j + 1 < block.size()) {
                            block.remove(j + 1);
                        }
                        break;
                    }
                }
            }
            Block lastBlock = blocks.getLast();
            if (lastBlock.isEmpty() || !(lastBlock.getLast() instanceof RetVIR)) {
                switch (func.getType()) {
                    case BasicType.I32 -> lastBlock.add(new RetVIR(new ConstantNumber(0)));
                    case BasicType.FLOAT -> lastBlock.add(new RetVIR(new ConstantNumber(0.0f)));
                    case BasicType.VOID -> lastBlock.add(new RetVIR(null));
                    default -> throw new IllegalStateException("Unexpected value: " + func.getType());
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
        for (SysYParser.BinaryExpContext exp : ctx.binaryExp()) {
            dimensions.add(calc(exp).intValue());
        }
        return dimensions;
    }

    @Override
    public Object visitVarDecl(SysYParser.VarDeclContext ctx) {
        isConst = ctx.CONST() != null;
        return super.visitVarDecl(ctx);
    }

    @Override
    public Object visitVarDef(SysYParser.VarDefContext ctx) {
        String name = ctx.Ident().getSymbol().getText();
        if (ctx.dimensions() != null) {
            List<Integer> dimensions = visitDimensions(ctx.dimensions());
            SysYParser.InitValContext initVal = ctx.initVal();
            if (isConst) {
                Map<Integer, SysYParser.BinaryExpContext> exps = new HashMap<>();
                allocInitVal(dimensions, exps, 0, initVal);
                Type type = curType;
                for (int dimension : dimensions.reversed())
                    type = new ArrayType(type, dimension);
                globals.add(switch (curType) {
                    case BasicType.I32 ->
                            symbolTable.makeConst(type, name, exps.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, exp -> calc(exp.getValue()).intValue())));
                    case BasicType.FLOAT ->
                            symbolTable.makeConst(type, name, exps.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, exp -> Float.floatToIntBits(calc(exp.getValue()).floatValue()))));
                    default -> throw new IllegalStateException("Unexpected value: " + curType);
                });
            } else if (symbolTable.size() == 1) {
                Map<Integer, SysYParser.BinaryExpContext> exps = new HashMap<>();
                if (initVal != null)
                    allocInitVal(dimensions, exps, 0, initVal);
                Type type = curType;
                for (int dimension : dimensions.reversed())
                    type = new ArrayType(type, dimension);
                globals.add(switch (curType) {
                    case BasicType.I32 ->
                            symbolTable.makeGlobal(type, name, exps.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, exp -> calc(exp.getValue()).intValue())));
                    case BasicType.FLOAT ->
                            symbolTable.makeGlobal(type, name, exps.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, exp -> Float.floatToIntBits(calc(exp.getValue()).floatValue()))));
                    default -> throw new IllegalStateException("Unexpected value: " + curType);
                });
            } else {
                AllocaVIR allocaVIR = symbolTable.makeLocal(curType, name, dimensions);
                allocaVIRs.add(allocaVIR);
                if (initVal != null) {
                    Map<Integer, SysYParser.BinaryExpContext> exps = new HashMap<>();
                    allocInitVal(dimensions, exps, 0, initVal);
                    BitCastVIR bitCastVIR = new BitCastVIR(new PointerType(BasicType.I32), allocaVIR);
                    curBlock.add(bitCastVIR);
                    curBlock.add(new CallVIR(symbolTable.getFunc("memset"), List.of(bitCastVIR, new ConstantNumber(0), new ConstantNumber(dimensions.stream().reduce(4, Math::multiplyExact)))));
                    int totalNum = dimensions.stream().reduce(1, Math::multiplyExact);
                    for (int i = 0; i < totalNum; i++) {
                        SysYParser.BinaryExpContext exp = exps.get(i);
                        if (exp != null) {
                            List<Integer> indexes = new ArrayList<>();
                            int rest = i;
                            for (int j = 0; j < dimensions.size(); j++) {
                                indexes.add(rest % dimensions.get(dimensions.size() - j - 1));
                                rest /= dimensions.get(dimensions.size() - j - 1);
                            }
                            Collections.reverse(indexes);
                            Value value = typeConversion(visitBinaryExp(exps.get(i)), curType);
                            Value pointer = allocaVIR;
                            for (int index : indexes) {
                                VIR ir = new GetElementPtrVIR(pointer, new ConstantNumber(0), new ConstantNumber(index));
                                curBlock.add(ir);
                                pointer = ir;
                            }
                            curBlock.add(new StoreVIR(value, pointer));
                        }
                    }
                }
            }
        } else {
            if (isConst) {
                Number value = calc(ctx.initVal().binaryExp());
                globals.add(switch (curType) {
                    case BasicType.I32 -> symbolTable.makeConst(BasicType.I32, name, value.intValue());
                    case BasicType.FLOAT -> symbolTable.makeConst(BasicType.FLOAT, name, value.floatValue());
                    default -> throw new IllegalStateException("Unexpected value: " + curType);
                });
            } else if (symbolTable.size() == 1) {
                Number value = switch (curType) {
                    case BasicType.I32 -> 0;
                    case BasicType.FLOAT -> 0.0f;
                    default -> throw new IllegalStateException("Unexpected value: " + curType);
                };
                if (ctx.initVal() != null)
                    value = calc(ctx.initVal().binaryExp());
                globals.add(switch (curType) {
                    case BasicType.I32 -> symbolTable.makeGlobal(BasicType.I32, name, value.intValue());
                    case BasicType.FLOAT -> symbolTable.makeGlobal(BasicType.FLOAT, name, value.floatValue());
                    default -> throw new IllegalStateException("Unexpected value: " + curType);
                });
            } else {
                AllocaVIR allocaVIR = symbolTable.makeLocal(curType, name);
                allocaVIRs.add(allocaVIR);
                SysYParser.InitValContext initVal = ctx.initVal();
                if (initVal != null) {
                    Value value = visitBinaryExp(initVal.binaryExp());
                    value = typeConversion(value, curType);
                    curBlock.add(new StoreVIR(value, allocaVIR));
                }
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
        for (SysYParser.FuncArgContext arg : ctx.funcArg())
            curFunc.addArg(visitFuncArg(arg));
        curBlock = new Block();
        curFunc.addBlock(curBlock);
        allocaVIRs = new ArrayList<>();
        for (Argument arg : curFunc.getArgs()) {
            AllocaVIR allocaVIR = symbolTable.makeLocal(arg.getType(), arg.getName());
            allocaVIRs.add(allocaVIR);
            curBlock.add(new StoreVIR(arg, allocaVIR));
            argToAllocaMap.put(arg, allocaVIR);
        }
        visitBlockStmt(ctx.blockStmt());
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
            for (SysYParser.BinaryExpContext exp : ctx.binaryExp().reversed())
                type = new ArrayType(type, calc(exp).intValue());
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
        Value value = visitBinaryExp(ctx.binaryExp());
        Type type = pointer.getType();
        if (type instanceof BasicType)
            value = typeConversion(value, type);
        else
            value = typeConversion(value, type.getBaseType());
        curBlock.add(new StoreVIR(value, pointer));
        return null;
    }

    @Override
    public Object visitIfStmt(SysYParser.IfStmtContext ctx) {
        Block trueBlock = new Block();
        Block falseBlock = new Block();
        if (ctx.stmt().size() == 2) {
            Block ifEndBlock = new Block();
            curFunc.insertBlockAfter(curBlock, trueBlock);
            curFunc.insertBlockAfter(trueBlock, falseBlock);
            curFunc.insertBlockAfter(falseBlock, ifEndBlock);
            this.trueBlock = trueBlock;
            this.falseBlock = falseBlock;
            calculatingCond = true;
            Value reg = visitBinaryExp(ctx.binaryExp());
            if (reg != null && reg.getType() != BasicType.VOID) {
                VIR cmpVIR = switch (reg.getType()) {
                    case BasicType.I32 -> new ICmpVIR(ICmpVIR.Cond.NE, reg, new ConstantNumber(0));
                    case BasicType.FLOAT -> new FCmpVIR(FCmpVIR.Cond.UNE, reg, new ConstantNumber(0.0f));
                    default -> throw new IllegalStateException("Unexpected value: " + reg.getType());
                };
                curBlock.add(cmpVIR);
                curBlock.add(new BranchVIR(cmpVIR, this.trueBlock, this.falseBlock));
            }
            calculatingCond = false;
            curBlock = trueBlock;
            visitStmt(ctx.stmt(0));
            curBlock.add(new BranchVIR(ifEndBlock));
            curBlock = falseBlock;
            visitStmt(ctx.stmt(1));
            curBlock.add(new BranchVIR(ifEndBlock));
            curBlock = ifEndBlock;
        } else {
            curFunc.insertBlockAfter(curBlock, trueBlock);
            curFunc.insertBlockAfter(trueBlock, falseBlock);
            this.trueBlock = trueBlock;
            this.falseBlock = falseBlock;
            calculatingCond = true;
            Value reg = visitBinaryExp(ctx.binaryExp());
            if (reg != null && reg.getType() != BasicType.VOID) {
                VIR cmpVIR = switch (reg.getType()) {
                    case BasicType.I32 -> new ICmpVIR(ICmpVIR.Cond.NE, reg, new ConstantNumber(0));
                    case BasicType.FLOAT -> new FCmpVIR(FCmpVIR.Cond.UNE, reg, new ConstantNumber(0.0f));
                    default -> throw new IllegalStateException("Unexpected value: " + reg.getType());
                };
                curBlock.add(cmpVIR);
                curBlock.add(new BranchVIR(cmpVIR, trueBlock, falseBlock));
            }
            calculatingCond = false;
            curBlock = trueBlock;
            visitStmt(ctx.stmt(0));
            curBlock.add(new BranchVIR(falseBlock));
            curBlock = falseBlock;
        }
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
        calculatingCond = true;
        Value reg = visitBinaryExp(ctx.binaryExp());
        if (reg != null && reg.getType() != BasicType.VOID) {
            VIR cmpVIR = switch (reg.getType()) {
                case BasicType.I32 -> new ICmpVIR(ICmpVIR.Cond.NE, reg, new ConstantNumber(0));
                case BasicType.FLOAT -> new FCmpVIR(FCmpVIR.Cond.UNE, reg, new ConstantNumber(0.0f));
                default -> throw new IllegalStateException("Unexpected value: " + reg.getType());
            };
            curBlock.add(cmpVIR);
            curBlock.add(new BranchVIR(cmpVIR, this.trueBlock, this.falseBlock));
        }
        calculatingCond = false;
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
        if (ctx.binaryExp() == null) {
            curBlock.add(new RetVIR(null));
            return null;
        }
        Value retReg = visitBinaryExp(ctx.binaryExp());
        retReg = typeConversion(retReg, curFunc.getType());
        curBlock.add(new RetVIR(retReg));
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
        if (ctx.binaryExp().isEmpty()) {
            return pointer;
        }
        if (pointer.getType() instanceof PointerType pointerType && pointerType.getBaseType() instanceof PointerType) {
            VIR ir = new LoadVIR(pointer);
            pointer = ir;
            curBlock.add(ir);
        }
        List<Value> dimensions = ctx.binaryExp().stream().map(this::visitBinaryExp).toList();
        boolean isFirst = true;
        for (Value dimension : dimensions) {
            VIR ir;
            if (isArg && isFirst)
                ir = new GetElementPtrVIR(pointer, dimension);
            else if (pointer.getType() instanceof PointerType pointerType && pointerType.getBaseType() instanceof BasicType)
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
        boolean calculatingCond = this.calculatingCond;
        this.calculatingCond = false;
        if (ctx.getChildCount() == 2) {
            if (calculatingCond) {
                if (ctx.getChild(0).getText().equals("!")) {
                    Block trueBlock = this.trueBlock;
                    Block falseBlock = this.falseBlock;
                    this.trueBlock = falseBlock;
                    this.falseBlock = trueBlock;
                }
                this.calculatingCond = calculatingCond;
                Value reg = visitUnaryExp(ctx.unaryExp());
                if (reg != null && reg.getType() != BasicType.VOID) {
                    VIR cmpVIR = switch (reg.getType()) {
                        case BasicType.I32 -> new ICmpVIR(ICmpVIR.Cond.NE, reg, new ConstantNumber(0));
                        case BasicType.FLOAT -> new FCmpVIR(FCmpVIR.Cond.UNE, reg, new ConstantNumber(0.0f));
                        default -> throw new IllegalStateException("Unexpected value: " + reg.getType());
                    };
                    curBlock.add(cmpVIR);
                    curBlock.add(new BranchVIR(cmpVIR, this.trueBlock, this.falseBlock));
                }
                return null;
            }
            Value value = visitUnaryExp(ctx.unaryExp());
            this.calculatingCond = calculatingCond;
            return switch (ctx.getChild(0).getText()) {
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
        if (ctx.getChildCount() == 3) {
            Value reg = visitBinaryExp(ctx.binaryExp());
            this.calculatingCond = calculatingCond;
            return reg;
        }
        if (ctx.IntConst() != null) {
            this.calculatingCond = calculatingCond;
            return new ConstantNumber(Integer.decode(ctx.IntConst().getSymbol().getText()));
        }
        if (ctx.FloatConst() != null) {
            this.calculatingCond = calculatingCond;
            return new ConstantNumber(Float.parseFloat(ctx.FloatConst().getSymbol().getText()));
        }
        Value result = (Value) visit(ctx.getChild(0));
        this.calculatingCond = calculatingCond;
        return result;
    }

    @Override
    public Value visitVarExp(SysYParser.VarExpContext ctx) {
        Value pointer = symbolTable.getData(ctx.Ident().getSymbol().getText());
        boolean isArg = false;
        if (pointer instanceof Argument) {
            isArg = true;
            pointer = argToAllocaMap.get(pointer);
        }
        Type type = pointer.getType();
        if (ctx.binaryExp().isEmpty()) {
            if (pointer instanceof GlobalVariable) {
                VIR ir;
                if (type instanceof ArrayType) {
                    int depth = 2;
                    while (type.getBaseType() instanceof ArrayType) {
                        depth++;
                        type = type.getBaseType();
                    }
                    Value[] indexes = new Value[depth];
                    Arrays.fill(indexes, new ConstantNumber(0));
                    ir = new GetElementPtrVIR(pointer, indexes);
                } else
                    ir = new LoadVIR(pointer);
                curBlock.add(ir);
                return ir;
            }
            if (type instanceof BasicType) {
                return pointer;
            }
            if (type instanceof PointerType && type.getBaseType() instanceof BasicType) {
                VIR newIR = new LoadVIR(pointer);
                curBlock.add(newIR);
                return newIR;
            }
            if (type instanceof PointerType && type.getBaseType() instanceof PointerType) {
                VIR newIR = new LoadVIR(pointer);
                curBlock.add(newIR);
                return newIR;
            }
            if (type instanceof PointerType && type.getBaseType() instanceof ArrayType) {
                int depth = 1;
                while (type.getBaseType() instanceof ArrayType) {
                    depth++;
                    type = type.getBaseType();
                }
                Value[] indexes = new Value[depth];
                Arrays.fill(indexes, new ConstantNumber(0));
                VIR ir = new GetElementPtrVIR(pointer, indexes);
                curBlock.add(ir);
                return ir;
            }
            return pointer;
        }
        if (pointer.getType() instanceof PointerType pointerType && pointerType.getBaseType() instanceof PointerType) {
            VIR ir = new LoadVIR(pointer);
            pointer = ir;
            curBlock.add(ir);
        }
        boolean isFirst = true;
        for (SysYParser.BinaryExpContext dimension : ctx.binaryExp()) {
            Value index = visitBinaryExp(dimension);
            index = typeConversion(index, BasicType.I32);
            VIR ir;
            if (isArg && isFirst)
                ir = new GetElementPtrVIR(pointer, index);
            else
                ir = new GetElementPtrVIR(pointer, new ConstantNumber(0), index);
            curBlock.add(ir);
            pointer = ir;
            isFirst = false;
        }
        if (pointer.getType().getBaseType() instanceof ArrayType) {
            return pointer;
        }
        VIR newIR = new LoadVIR(pointer);
        curBlock.add(newIR);
        return newIR;
    }

    @Override
    public Value visitFuncCallExp(SysYParser.FuncCallExpContext ctx) {
        VirtualFunction func = symbolTable.getFunc(ctx.Ident().getSymbol().getText());
        List<Value> params = new ArrayList<>();
        for (int i = 0; i < ctx.binaryExp().size(); i++) {
            SysYParser.BinaryExpContext exp = ctx.binaryExp().get(i);
            Value param = visitBinaryExp(exp);
            Type type = func.getArgs().get(i).getType() instanceof BasicType && func.getArgs().get(i).getType() == BasicType.FLOAT ? BasicType.FLOAT : BasicType.I32;
            param = typeConversion(param, type);
            params.add(param);
        }
        VIR newIR = new CallVIR(func, params);
        curBlock.add(newIR);
        return newIR;
    }

    @Override
    public Value visitBinaryExp(SysYParser.BinaryExpContext ctx) {
        if (ctx.getChildCount() == 1)
            return visitUnaryExp(ctx.unaryExp());
        if (ctx.getChild(1).getText().equals("||")) {
            Block lBlock = curBlock;
            Block rBlock = new Block();
            curFunc.insertBlockAfter(lBlock, rBlock);
            Block trueBlock = this.trueBlock;
            Block falseBlock = this.falseBlock;
            this.curBlock = lBlock;
            this.trueBlock = trueBlock;
            this.falseBlock = rBlock;
            Value lVal = visitBinaryExp(ctx.binaryExp(0));
            if (lVal != null && lVal.getType() != BasicType.VOID) {
                VIR cmpVIR = switch (lVal.getType()) {
                    case BasicType.I32 -> new ICmpVIR(ICmpVIR.Cond.NE, lVal, new ConstantNumber(0));
                    case BasicType.FLOAT -> new FCmpVIR(FCmpVIR.Cond.UNE, lVal, new ConstantNumber(0.0f));
                    default -> throw new IllegalStateException("Unexpected value: " + lVal.getType());
                };
                curBlock.add(cmpVIR);
                curBlock.add(new BranchVIR(cmpVIR, this.trueBlock, this.falseBlock));
            }
            this.curBlock = rBlock;
            this.trueBlock = trueBlock;
            this.falseBlock = falseBlock;
            Value rVal = visitBinaryExp(ctx.binaryExp(1));
            if (rVal != null && rVal.getType() != BasicType.VOID) {
                VIR cmpVIR = switch (rVal.getType()) {
                    case BasicType.I32 -> new ICmpVIR(ICmpVIR.Cond.NE, rVal, new ConstantNumber(0));
                    case BasicType.FLOAT -> new FCmpVIR(FCmpVIR.Cond.UNE, rVal, new ConstantNumber(0.0f));
                    default -> throw new IllegalStateException("Unexpected value: " + rVal.getType());
                };
                curBlock.add(cmpVIR);
                curBlock.add(new BranchVIR(cmpVIR, this.trueBlock, this.falseBlock));
            }
            return null;
        }
        if (ctx.getChild(1).getText().equals("&&")) {
            Block lBlock = curBlock;
            Block rBlock = new Block();
            curFunc.insertBlockAfter(lBlock, rBlock);
            Block trueBlock = this.trueBlock;
            Block falseBlock = this.falseBlock;
            this.curBlock = lBlock;
            this.trueBlock = rBlock;
            this.falseBlock = falseBlock;
            Value lVal = visitBinaryExp(ctx.binaryExp(0));
            if (lVal != null && lVal.getType() != BasicType.VOID) {
                VIR cmpVIR = switch (lVal.getType()) {
                    case BasicType.I32 -> new ICmpVIR(ICmpVIR.Cond.NE, lVal, new ConstantNumber(0));
                    case BasicType.FLOAT -> new FCmpVIR(FCmpVIR.Cond.UNE, lVal, new ConstantNumber(0.0f));
                    default -> throw new IllegalStateException("Unexpected value: " + lVal.getType());
                };
                curBlock.add(cmpVIR);
                curBlock.add(new BranchVIR(cmpVIR, this.trueBlock, this.falseBlock));
            }
            this.curBlock = rBlock;
            this.trueBlock = trueBlock;
            this.falseBlock = falseBlock;
            Value rVal = visitBinaryExp(ctx.binaryExp(1));
            if (rVal != null && rVal.getType() != BasicType.VOID) {
                VIR cmpVIR = switch (rVal.getType()) {
                    case BasicType.I32 -> new ICmpVIR(ICmpVIR.Cond.NE, rVal, new ConstantNumber(0));
                    case BasicType.FLOAT -> new FCmpVIR(FCmpVIR.Cond.UNE, rVal, new ConstantNumber(0.0f));
                    default -> throw new IllegalStateException("Unexpected value: " + rVal.getType());
                };
                curBlock.add(cmpVIR);
                curBlock.add(new BranchVIR(cmpVIR, this.trueBlock, this.falseBlock));
            }
            return null;
        }
        boolean calculatingCond = this.calculatingCond;
        this.calculatingCond = false;
        Value lReg = visitBinaryExp(ctx.binaryExp(0));
        Value rReg = visitBinaryExp(ctx.binaryExp(1));
        Type targetType = automaticTypePromotion(lReg.getType(), rReg.getType());
        lReg = typeConversion(lReg, targetType);
        rReg = typeConversion(rReg, targetType);
        String op = ctx.getChild(1).getText();
        VIR result = switch (op) {
            case "+", "-", "*", "/", "%" -> {
                VIR ir = new BinaryVIR(switch (op) {
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
                    default -> throw new IllegalStateException("Unexpected value: " + op);
                }, lReg, rReg);
                curBlock.add(ir);
                yield ir;
            }
            case "==", "!=", ">=", ">", "<=", "<" -> {
                VIR ir = switch (targetType) {
                    case BasicType.I32 -> new ICmpVIR(switch (op) {
                        case "==" -> ICmpVIR.Cond.EQ;
                        case "!=" -> ICmpVIR.Cond.NE;
                        case ">=" -> ICmpVIR.Cond.SGE;
                        case ">" -> ICmpVIR.Cond.SGT;
                        case "<=" -> ICmpVIR.Cond.SLE;
                        case "<" -> ICmpVIR.Cond.SLT;
                        default -> throw new IllegalStateException("Unexpected value: " + op);
                    }, lReg, rReg);
                    case BasicType.FLOAT -> new FCmpVIR(switch (op) {
                        case "==" -> FCmpVIR.Cond.OEQ;
                        case "!=" -> FCmpVIR.Cond.UNE;
                        case ">=" -> FCmpVIR.Cond.OGE;
                        case ">" -> FCmpVIR.Cond.OGT;
                        case "<=" -> FCmpVIR.Cond.OLE;
                        case "<" -> FCmpVIR.Cond.OLT;
                        default -> throw new IllegalStateException("Unexpected value: " + op);
                    }, lReg, rReg);
                    default -> throw new IllegalStateException("Unexpected value: " + targetType);
                };
                curBlock.add(ir);
                if (calculatingCond) {
                    ir = new BranchVIR(ir, trueBlock, falseBlock);
                    curBlock.add(ir);
                }
                yield ir;
            }
            default -> throw new IllegalStateException("Unexpected value: " + ctx.getChild(1).getText());
        };
        this.calculatingCond = calculatingCond;
        return result;
    }

    private void allocInitVal(List<Integer> dimensions, Map<Integer, SysYParser.BinaryExpContext> exps, int base, SysYParser.InitValContext src) {
        int offset = 0;
        for (SysYParser.InitValContext exp : src.initVal()) {
            if (exp.binaryExp() == null) {
                int size = dimensions.stream().skip(1).reduce(1, Math::multiplyExact);
                offset = (offset + size - 1) / size * size;
                allocInitVal(dimensions.subList(1, dimensions.size()), exps, base + offset, exp);
                offset += size;
            } else {
                exps.put(base + offset, exp.binaryExp());
                offset++;
            }
        }
    }

    private Number calc(ParserRuleContext ctx) {
        if (ctx instanceof SysYParser.BinaryExpContext binaryExp) {
            if (binaryExp.unaryExp() != null)
                return calc(binaryExp.unaryExp());
            Number lVal = calc(binaryExp.binaryExp(0));
            Number rVal = calc(binaryExp.binaryExp(1));
            return BIOP_MAP.get(binaryExp.getChild(1).getText()).apply(lVal, rVal);
        }
        if (ctx instanceof SysYParser.UnaryExpContext unaryExp) {
            if (unaryExp.varExp() != null)
                return calc(unaryExp.varExp());
            if (unaryExp.IntConst() != null)
                return Integer.decode(unaryExp.IntConst().getText());
            if (unaryExp.FloatConst() != null)
                return Float.parseFloat(unaryExp.FloatConst().getText());
            Number val = calc(unaryExp.unaryExp());
            return UOP_MAP.get(unaryExp.getChild(0).getText()).apply(val);
        }
        if (ctx instanceof SysYParser.VarExpContext varExp) {
            String name = varExp.Ident().getSymbol().getText();
            Value symbol = symbolTable.getData(name);
            if (symbol instanceof GlobalVariable global) {
                if (varExp.binaryExp().isEmpty()) {
                    if (global.getType() == BasicType.FLOAT)
                        return global.getFloat();
                    return global.getInt();
                }
                if (global.getDimensions().size() != varExp.binaryExp().size())
                    throw new RuntimeException();
                int offset = 0;
                int[] sizes = global.getSizes();
                for (int i = 0; i < varExp.binaryExp().size(); i++)
                    offset += sizes[i] * calc(varExp.binaryExp().get(i)).intValue();
                if (global.getType() == BasicType.FLOAT)
                    return global.getFloat(offset);
                return global.getInt(offset);
            }
            throw new RuntimeException();
        }
        throw new RuntimeException();
    }

    private static Type automaticTypePromotion(Type type1, Type type2) {
        if (type1 == BasicType.FLOAT || type2 == BasicType.FLOAT)
            return BasicType.FLOAT;
        if (type1 == BasicType.I32 || type2 == BasicType.I32)
            return BasicType.I32;
        return BasicType.I1;
    }

    private Value typeConversion(Value value, Type targetType) {
        if (value.getType() == targetType)
            return value;
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
}
