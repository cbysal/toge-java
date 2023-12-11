package compile;

import common.NumberUtils;
import compile.symbol.*;
import compile.sysy.SysYBaseVisitor;
import compile.sysy.SysYParser;
import compile.vir.Block;
import compile.vir.Argument;
import compile.vir.VirtualFunction;
import compile.vir.ir.*;
import compile.vir.type.ArrayType;
import compile.vir.type.BasicType;
import compile.vir.type.PointerType;
import compile.vir.type.Type;
import compile.vir.value.Value;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class VIRGenerator extends SysYBaseVisitor<Object> {
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
    private final Set<GlobalSymbol> globals = new HashSet<>();
    private final Map<String, VirtualFunction> funcs = new HashMap<>();
    private final SymbolTable symbolTable = new SymbolTable();
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

    public Set<GlobalSymbol> getGlobals() {
        checkIfIsProcessed();
        return globals;
    }

    private void formatVIRs() {
        for (VirtualFunction func : funcs.values()) {
            List<Block> blocks = func.getBlocks();
            for (int i = 0; i + 1 < blocks.size(); i++) {
                Block block = blocks.get(i);
                VIR lastIR = block.getLast();
                if (!(lastIR instanceof BranchVIR || lastIR instanceof JumpVIR || lastIR instanceof RetVIR))
                    block.add(new JumpVIR(blocks.get(i + 1)));
            }
            for (int i = 0; i + 1 < blocks.size(); i++) {
                Block block = blocks.get(i);
                for (int j = 0; j + 1 < block.size(); j++) {
                    VIR ir = block.get(j);
                    if (ir instanceof JumpVIR || ir instanceof RetVIR) {
                        while (j + 1 < block.size()) {
                            block.remove(j + 1);
                        }
                        break;
                    }
                }
            }
        }
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
                globals.add(switch (curType) {
                    case BasicType.I32 ->
                            symbolTable.makeConst(BasicType.I32, name, dimensions, exps.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, exp -> calc(exp.getValue()).intValue())));
                    case BasicType.FLOAT ->
                            symbolTable.makeConst(BasicType.FLOAT, name, dimensions, exps.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, exp -> Float.floatToIntBits(calc(exp.getValue()).floatValue()))));
                    default -> throw new IllegalStateException("Unexpected value: " + curType);
                });
            } else if (symbolTable.size() == 1) {
                Map<Integer, SysYParser.BinaryExpContext> exps = new HashMap<>();
                if (initVal != null)
                    allocInitVal(dimensions, exps, 0, initVal);
                globals.add(switch (curType) {
                    case BasicType.I32 ->
                            symbolTable.makeGlobal(BasicType.I32, name, dimensions, exps.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, exp -> calc(exp.getValue()).intValue())));
                    case BasicType.FLOAT ->
                            symbolTable.makeGlobal(BasicType.FLOAT, name, dimensions, exps.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, exp -> Float.floatToIntBits(calc(exp.getValue()).floatValue()))));
                    default -> throw new IllegalStateException("Unexpected value: " + curType);
                });
            } else {
                AllocaVIR allocaVIR = symbolTable.makeLocal(curType, name, dimensions);
                allocaVIRs.add(allocaVIR);
                if (initVal != null) {
                    Map<Integer, SysYParser.BinaryExpContext> exps = new HashMap<>();
                    allocInitVal(dimensions, exps, 0, initVal);
                    curBlock.add(new CallVIR(symbolTable.getFunc("memset"), List.of(allocaVIR, new InstantValue(0), new InstantValue(dimensions.stream().reduce(4, Math::multiplyExact)))));
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
                                VIR ir = new GetElementPtrVIR(pointer, new InstantValue(0), new InstantValue(index));
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
        Type funcType = visitType(ctx.type());
        FuncSymbol func = symbolTable.makeFunc(funcType, ctx.Ident().getSymbol().getText());
        symbolTable.in();
        for (SysYParser.FuncArgContext arg : ctx.funcArg())
            func.addArg(visitFuncArg(arg));
        curFunc = new VirtualFunction(func);
        curBlock = new Block();
        curFunc.addBlock(curBlock);
        allocaVIRs = new ArrayList<>();
        for (Argument arg : curFunc.getSymbol().getArgs()) {
            AllocaVIR allocaVIR = symbolTable.makeLocal(arg.getType(), arg.getName());
            allocaVIRs.add(allocaVIR);
            curBlock.add(new StoreVIR(arg, allocaVIR));
        }
        visitBlockStmt(ctx.blockStmt());
        curFunc.getBlocks().getFirst().addAll(0, allocaVIRs);
        funcs.put(func.getName(), curFunc);
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
        Pair<Value, List<Value>> lValUnit = visitLVal(ctx.lVal());
        Value value = visitBinaryExp(ctx.binaryExp());
        if (lValUnit.getLeft() instanceof AllocaVIR) {
            Type targetType = lValUnit.getLeft().getType();
            for (int i = 0; i < lValUnit.getRight().size(); i++) {
                if (targetType instanceof BasicType)
                    throw new RuntimeException();
                targetType = targetType.getBaseType();
            }
            value = typeConversion(value, targetType);
        } else {
            value = typeConversion(value, lValUnit.getLeft().getType());
        }
        Value pointer = lValUnit.getLeft();
        Type type = pointer.getType();
        if (pointer instanceof GlobalSymbol symbol) {
            for (int i = symbol.getDimensionSize() - 1; i >= 0; i--) {
                type = new ArrayType(type, symbol.getDimensions().get(i));
            }
            type = new PointerType(type);
        }
        if (type.getBaseType() instanceof PointerType) {
            boolean isFirst = true;
            for (Value index : lValUnit.getRight()) {
                if (isFirst) {
                    VIR loadIR = new LoadVIR(pointer);
                    curBlock.add(loadIR);
                    pointer = loadIR;
                    VIR ir = new GetElementPtrVIR(pointer, index);
                    curBlock.add(ir);
                    pointer = ir;
                } else {
                    VIR ir = new GetElementPtrVIR(pointer, new InstantValue(0), index);
                    curBlock.add(ir);
                    pointer = ir;
                }
                isFirst = false;
            }
        } else {
            for (Value index : lValUnit.getRight()) {
                VIR ir = new GetElementPtrVIR(pointer, new InstantValue(0), index);
                curBlock.add(ir);
                pointer = ir;
            }
        }
        for (int i = 0; i < lValUnit.getRight().size(); i++) {
            type = type.getBaseType();
        }
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
            if (reg != null) {
                curBlock.add(new BranchVIR(BranchVIR.Type.NE, reg, new InstantValue(0), this.trueBlock, this.falseBlock));
            }
            calculatingCond = false;
            curBlock = trueBlock;
            visitStmt(ctx.stmt(0));
            curBlock.add(new JumpVIR(ifEndBlock));
            curBlock = falseBlock;
            visitStmt(ctx.stmt(1));
            curBlock.add(new JumpVIR(ifEndBlock));
            curBlock = ifEndBlock;
        } else {
            curFunc.insertBlockAfter(curBlock, trueBlock);
            curFunc.insertBlockAfter(trueBlock, falseBlock);
            this.trueBlock = trueBlock;
            this.falseBlock = falseBlock;
            calculatingCond = true;
            Value reg = visitBinaryExp(ctx.binaryExp());
            if (reg != null) {
                curBlock.add(new BranchVIR(BranchVIR.Type.NE, reg, new InstantValue(0), trueBlock, falseBlock));
            }
            calculatingCond = false;
            curBlock = trueBlock;
            visitStmt(ctx.stmt(0));
            curBlock.add(new JumpVIR(falseBlock));
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
        curBlock.add(new JumpVIR(entryBlock));
        curBlock = entryBlock;
        trueBlock = loopBlock;
        falseBlock = endBlock;
        calculatingCond = true;
        Value reg = visitBinaryExp(ctx.binaryExp());
        if (reg != null) {
            curBlock.add(new BranchVIR(BranchVIR.Type.NE, reg, new InstantValue(0), this.trueBlock, this.falseBlock));
        }
        calculatingCond = false;
        curBlock = loopBlock;
        visitStmt(ctx.stmt());
        curBlock.add(new JumpVIR(entryBlock));
        curBlock = endBlock;
        continueStack.pop();
        breakStack.pop();
        return null;
    }

    @Override
    public Object visitBreakStmt(SysYParser.BreakStmtContext ctx) {
        curBlock.add(new JumpVIR(breakStack.peek()));
        return null;
    }

    @Override
    public Object visitContinueStmt(SysYParser.ContinueStmtContext ctx) {
        curBlock.add(new JumpVIR(continueStack.peek()));
        return null;
    }

    @Override
    public Object visitRetStmt(SysYParser.RetStmtContext ctx) {
        if (ctx.binaryExp() == null) {
            curBlock.add(new RetVIR(null));
            return null;
        }
        Value retReg = visitBinaryExp(ctx.binaryExp());
        retReg = typeConversion(retReg, curFunc.getSymbol().getType());
        curBlock.add(new RetVIR(retReg));
        return null;
    }

    @Override
    public Pair<Value, List<Value>> visitLVal(SysYParser.LValContext ctx) {
        Value symbol = symbolTable.getData(ctx.Ident().getSymbol().getText());
        if (ctx.binaryExp().isEmpty())
            return Pair.of(symbol, List.of());
        List<Value> dimensions = ctx.binaryExp().stream().map(this::visitBinaryExp).collect(Collectors.toList());
        return Pair.of(symbol, dimensions);
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
                if (reg != null) {
                    curBlock.add(new BranchVIR(BranchVIR.Type.NE, reg, new InstantValue(0), this.trueBlock, this.falseBlock));
                }
                return null;
            }
            Value reg = visitUnaryExp(ctx.unaryExp());
            this.calculatingCond = calculatingCond;
            return switch (ctx.getChild(0).getText()) {
                case "+" -> reg;
                case "-" -> {
                    VIR ir = new UnaryVIR(UnaryVIR.Type.NEG, reg);
                    curBlock.add(ir);
                    yield ir;
                }
                case "!" -> {
                    VIR ir = new UnaryVIR(UnaryVIR.Type.L_NOT, reg);
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
            VIR ir = new LiVIR(Integer.decode(ctx.IntConst().getSymbol().getText()));
            curBlock.add(ir);
            this.calculatingCond = calculatingCond;
            return ir;
        }
        if (ctx.FloatConst() != null) {
            VIR ir = new LiVIR(Float.parseFloat(ctx.FloatConst().getSymbol().getText()));
            curBlock.add(ir);
            this.calculatingCond = calculatingCond;
            return ir;
        }
        Value result = (Value) visit(ctx.getChild(0));
        this.calculatingCond = calculatingCond;
        return result;
    }

    @Override
    public Value visitVarExp(SysYParser.VarExpContext ctx) {
        Value pointer = symbolTable.getData(ctx.Ident().getSymbol().getText());
        Type type = pointer.getType();
        if (pointer instanceof GlobalSymbol symbol) {
            for (int i = symbol.getDimensionSize() - 1; i >= 0; i--) {
                type = new ArrayType(type, symbol.getDimensions().get(i));
            }
            type = new PointerType(type);
        }
        if (ctx.binaryExp().isEmpty()) {
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
            return pointer;
        }
        if (type.getBaseType() instanceof PointerType) {
            boolean isFirst = true;
            for (SysYParser.BinaryExpContext dimension : ctx.binaryExp()) {
                Value index = visitBinaryExp(dimension);
                index = typeConversion(index, BasicType.I32);
                if (isFirst) {
                    VIR loadVIR = new LoadVIR(pointer);
                    curBlock.add(loadVIR);
                    pointer = loadVIR;
                    VIR ir = new GetElementPtrVIR(pointer, index);
                    curBlock.add(ir);
                    pointer = ir;
                } else {
                    VIR ir = new GetElementPtrVIR(pointer, new InstantValue(0), index);
                    curBlock.add(ir);
                    pointer = ir;
                }
                isFirst = false;
            }
        } else {
            for (SysYParser.BinaryExpContext dimension : ctx.binaryExp()) {
                Value index = visitBinaryExp(dimension);
                index = typeConversion(index, BasicType.I32);
                VIR ir = new GetElementPtrVIR(pointer, new InstantValue(0), index);
                curBlock.add(ir);
                pointer = ir;
            }
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
        FuncSymbol symbol = symbolTable.getFunc(ctx.Ident().getSymbol().getText());
        List<Value> params = new ArrayList<>();
        for (int i = 0; i < ctx.binaryExp().size(); i++) {
            SysYParser.BinaryExpContext exp = ctx.binaryExp().get(i);
            Value param = visitBinaryExp(exp);
            Type type = symbol.getArgs().get(i).getType() instanceof BasicType && symbol.getArgs().get(i).getType() == BasicType.FLOAT ? BasicType.FLOAT : BasicType.I32;
            param = typeConversion(param, type);
            params.add(param);
        }
        VIR newIR = new CallVIR(symbol, params);
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
            if (lVal != null) {
                curBlock.add(new BranchVIR(BranchVIR.Type.NE, lVal, new InstantValue(0), this.trueBlock, this.falseBlock));
            }
            this.curBlock = rBlock;
            this.trueBlock = trueBlock;
            this.falseBlock = falseBlock;
            Value rVal = visitBinaryExp(ctx.binaryExp(1));
            if (rVal != null) {
                curBlock.add(new BranchVIR(BranchVIR.Type.NE, rVal, new InstantValue(0), this.trueBlock, this.falseBlock));
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
            if (lVal != null) {
                curBlock.add(new BranchVIR(BranchVIR.Type.NE, lVal, new InstantValue(0), this.trueBlock, this.falseBlock));
            }
            this.curBlock = rBlock;
            this.trueBlock = trueBlock;
            this.falseBlock = falseBlock;
            Value rVal = visitBinaryExp(ctx.binaryExp(1));
            if (rVal != null) {
                curBlock.add(new BranchVIR(BranchVIR.Type.NE, rVal, new InstantValue(0), this.trueBlock, this.falseBlock));
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
                    case "+" -> BinaryVIR.Type.ADD;
                    case "-" -> BinaryVIR.Type.SUB;
                    case "*" -> BinaryVIR.Type.MUL;
                    case "/" -> BinaryVIR.Type.DIV;
                    case "%" -> BinaryVIR.Type.MOD;
                    default -> throw new IllegalStateException("Unexpected value: " + op);
                }, lReg, rReg);
                curBlock.add(ir);
                yield ir;
            }
            case "==", "!=", ">=", ">", "<=", "<" -> {
                VIR ir = calculatingCond ? new BranchVIR(switch (op) {
                    case "==" -> BranchVIR.Type.EQ;
                    case "!=" -> BranchVIR.Type.NE;
                    case ">=" -> BranchVIR.Type.GE;
                    case ">" -> BranchVIR.Type.GT;
                    case "<=" -> BranchVIR.Type.LE;
                    case "<" -> BranchVIR.Type.LT;
                    default -> throw new IllegalStateException("Unexpected value: " + op);
                }, lReg, rReg, trueBlock, falseBlock) : new BinaryVIR(switch (op) {
                    case "==" -> BinaryVIR.Type.EQ;
                    case "!=" -> BinaryVIR.Type.NE;
                    case ">=" -> BinaryVIR.Type.GE;
                    case ">" -> BinaryVIR.Type.GT;
                    case "<=" -> BinaryVIR.Type.LE;
                    case "<" -> BinaryVIR.Type.LT;
                    default -> throw new IllegalStateException("Unexpected value: " + op);
                }, lReg, rReg);
                curBlock.add(ir);
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
            if (symbol instanceof GlobalSymbol globalSymbol) {
                if (varExp.binaryExp().isEmpty()) {
                    if (globalSymbol.getType() == BasicType.FLOAT)
                        return globalSymbol.getFloat();
                    return globalSymbol.getInt();
                }
                if (globalSymbol.getDimensionSize() != varExp.binaryExp().size())
                    throw new RuntimeException();
                int offset = 0;
                int[] sizes = globalSymbol.getSizes();
                for (int i = 0; i < varExp.binaryExp().size(); i++)
                    offset += sizes[i] * calc(varExp.binaryExp().get(i)).intValue();
                if (globalSymbol.getType() == BasicType.FLOAT)
                    return globalSymbol.getFloat(offset);
                return globalSymbol.getInt(offset);
            }
            throw new RuntimeException();
        }
        throw new RuntimeException();
    }

    private static Type automaticTypePromotion(Type type1, Type type2) {
        return type1 == BasicType.FLOAT || type2 == BasicType.FLOAT ? BasicType.FLOAT : BasicType.I32;
    }

    private Value typeConversion(Value value, Type targetType) {
        if (value.getType() == targetType)
            return value;
        if (value.getType() == BasicType.FLOAT && targetType == BasicType.I32) {
            VIR ir = new UnaryVIR(UnaryVIR.Type.F2I, value);
            curBlock.add(ir);
            value = ir;
        }
        if (value.getType() == BasicType.I32 && targetType == BasicType.FLOAT) {
            VIR ir = new UnaryVIR(UnaryVIR.Type.I2F, value);
            curBlock.add(ir);
            value = ir;
        }
        return value;
    }
}
