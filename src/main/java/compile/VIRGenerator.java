package compile;

import common.NumberUtils;
import compile.vir.Block;
import compile.vir.VReg;
import compile.vir.VirtualFunction;
import compile.vir.ir.*;
import compile.vir.type.BasicType;
import compile.vir.type.Type;
import compile.symbol.*;
import compile.sysy.SysYBaseVisitor;
import compile.sysy.SysYParser;
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
    private Block curBlock, trueBlock, falseBlock, retBlock;
    private VReg retVal;
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
                LocalSymbol localSymbol = symbolTable.makeLocal(curType, name, dimensions);
                curFunc.addLocal(localSymbol);
                if (initVal != null) {
                    Map<Integer, SysYParser.BinaryExpContext> exps = new HashMap<>();
                    allocInitVal(dimensions, exps, 0, initVal);
                    VReg symbolReg = new VReg(BasicType.I32, 8);
                    curBlock.add(new LoadVIR(symbolReg, localSymbol, List.of()));
                    curBlock.add(new CallVIR(symbolTable.getFunc("memset"), null, List.of(symbolReg, new InstantValue(0), new InstantValue(dimensions.stream().reduce(4, Math::multiplyExact)))));
                    int totalNum = dimensions.stream().reduce(1, Math::multiplyExact);
                    for (int i = 0; i < totalNum; i++) {
                        SysYParser.BinaryExpContext exp = exps.get(i);
                        if (exp != null) {
                            VReg reg = typeConversion(visitBinaryExp(exps.get(i)), curType);
                            List<VIRItem> items = new ArrayList<>();
                            int rest = i;
                            for (int j = 0; j < dimensions.size(); j++) {
                                items.add(new InstantValue(rest % dimensions.get(dimensions.size() - j - 1)));
                                rest /= dimensions.get(dimensions.size() - j - 1);
                            }
                            Collections.reverse(items);
                            curBlock.add(new StoreVIR(localSymbol, items, reg));
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
                LocalSymbol localSymbol = symbolTable.makeLocal(switch (curType) {
                    case BasicType.I32 -> BasicType.I32;
                    case BasicType.FLOAT -> BasicType.FLOAT;
                    default -> throw new IllegalStateException("Unexpected value: " + curType);
                }, name);
                curFunc.addLocal(localSymbol);
                SysYParser.InitValContext initVal = ctx.initVal();
                if (initVal != null) {
                    VReg reg = visitBinaryExp(initVal.binaryExp());
                    reg = typeConversion(reg, curType);
                    curBlock.add(new StoreVIR(localSymbol, List.of(), reg));
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
        for (SysYParser.FuncFParamContext param : ctx.funcFParam())
            func.addParam(visitFuncFParam(param));
        curFunc = new VirtualFunction(func);
        retBlock = new Block();
        retVal = switch (func.getType()) {
            case BasicType.FLOAT, BasicType.I32 -> new VReg(funcType, 4);
            case BasicType.VOID -> null;
            default -> throw new IllegalStateException("Unexpected value: " + func.getType());
        };
        retBlock.add(new RetVIR(retVal));
        curBlock = new Block();
        curFunc.addBlock(curBlock);
        visitBlockStmt(ctx.blockStmt());
        curFunc.addBlock(retBlock);
        funcs.put(func.getName(), curFunc);
        symbolTable.out();
        return null;
    }

    @Override
    public ParamSymbol visitFuncFParam(SysYParser.FuncFParamContext ctx) {
        visitType(ctx.type());
        Type paramType = visitType(ctx.type());
        List<Integer> dimensions = new ArrayList<>();
        if (!ctx.LB().isEmpty())
            dimensions.add(-1);
        for (SysYParser.BinaryExpContext exp : ctx.binaryExp())
            dimensions.add(calc(exp).intValue());
        return symbolTable.makeParam(paramType, ctx.Ident().getSymbol().getText(), dimensions);
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
        Pair<DataSymbol, List<VIRItem>> lValUnit = visitLVal(ctx.lVal());
        VReg rReg = visitBinaryExp(ctx.binaryExp());
        rReg = typeConversion(rReg, lValUnit.getLeft().getType());
        curBlock.add(new StoreVIR(lValUnit.getLeft(), lValUnit.getRight(), rReg));
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
            VReg reg = visitBinaryExp(ctx.binaryExp());
            if (reg != null) {
                VReg zero = new VReg(reg.getType(), reg.getSize());
                curBlock.add(new LiVIR(zero, 0));
                curBlock.add(new BranchVIR(BranchVIR.Type.NE, reg, zero, this.trueBlock, this.falseBlock));
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
            VReg reg = visitBinaryExp(ctx.binaryExp());
            if (reg != null) {
                VReg zero = new VReg(reg.getType(), reg.getSize());
                curBlock.add(new LiVIR(zero, 0));
                curBlock.add(new BranchVIR(BranchVIR.Type.NE, reg, zero, trueBlock, falseBlock));
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
        VReg reg = visitBinaryExp(ctx.binaryExp());
        if (reg != null) {
            VReg zero = new VReg(reg.getType(), reg.getSize());
            curBlock.add(new LiVIR(zero, 0));
            curBlock.add(new BranchVIR(BranchVIR.Type.NE, reg, zero, this.trueBlock, this.falseBlock));
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
            curBlock.add(new JumpVIR(retBlock));
            return null;
        }
        VReg retReg = visitBinaryExp(ctx.binaryExp());
        retReg = typeConversion(retReg, retVal.getType());
        curBlock.add(new MovVIR(retVal, retReg));
        curBlock.add(new JumpVIR(retBlock));
        return null;
    }

    @Override
    public Pair<DataSymbol, List<VIRItem>> visitLVal(SysYParser.LValContext ctx) {
        DataSymbol symbol = symbolTable.getData(ctx.Ident().getSymbol().getText());
        if (ctx.binaryExp().isEmpty())
            return Pair.of(symbol, List.of());
        List<VIRItem> dimensions = ctx.binaryExp().stream().map(this::visitBinaryExp).collect(Collectors.toList());
        return Pair.of(symbol, dimensions);
    }

    @Override
    public VReg visitUnaryExp(SysYParser.UnaryExpContext ctx) {
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
                VReg reg = visitUnaryExp(ctx.unaryExp());
                if (reg != null) {
                    VReg zero = new VReg(reg.getType(), reg.getSize());
                    curBlock.add(new LiVIR(zero, 0));
                    curBlock.add(new BranchVIR(BranchVIR.Type.NE, reg, zero, this.trueBlock, this.falseBlock));
                }
                return null;
            }
            VReg reg = visitUnaryExp(ctx.unaryExp());
            this.calculatingCond = calculatingCond;
            return switch (ctx.getChild(0).getText()) {
                case "+" -> reg;
                case "-" -> {
                    VReg result = new VReg(reg.getType(), reg.getSize());
                    curBlock.add(new UnaryVIR(UnaryVIR.Type.NEG, result, reg));
                    yield result;
                }
                case "!" -> {
                    VReg result = new VReg(BasicType.I32, 4);
                    curBlock.add(new UnaryVIR(UnaryVIR.Type.L_NOT, result, reg));
                    yield result;
                }
                default -> throw new IllegalStateException("Unexpected value: " + ctx.getChild(0).getText());
            };
        }
        if (ctx.getChildCount() == 3) {
            VReg reg = visitBinaryExp(ctx.binaryExp());
            this.calculatingCond = calculatingCond;
            return reg;
        }
        if (ctx.IntConst() != null) {
            VReg reg = new VReg(BasicType.I32, 4);
            curBlock.add(new LiVIR(reg, Integer.decode(ctx.IntConst().getSymbol().getText())));
            this.calculatingCond = calculatingCond;
            return reg;
        }
        if (ctx.FloatConst() != null) {
            VReg reg = new VReg(BasicType.FLOAT, 4);
            curBlock.add(new LiVIR(reg, Float.parseFloat(ctx.FloatConst().getSymbol().getText())));
            this.calculatingCond = calculatingCond;
            return reg;
        }
        VReg reg = (VReg) visit(ctx.getChild(0));
        this.calculatingCond = calculatingCond;
        return reg;
    }

    @Override
    public VReg visitVarExp(SysYParser.VarExpContext ctx) {
        DataSymbol symbol = symbolTable.getData(ctx.Ident().getSymbol().getText());
        VReg result = new VReg(ctx.binaryExp().size() == symbol.getDimensionSize() && symbol.getType() == BasicType.FLOAT ? BasicType.FLOAT : BasicType.I32, ctx.binaryExp().size() == symbol.getDimensionSize() || symbol.getType() == BasicType.FLOAT ? 4 : 8);
        if (ctx.binaryExp().isEmpty()) {
            curBlock.add(new LoadVIR(result, symbol, List.of()));
            return result;
        }
        List<VIRItem> dimensions = new ArrayList<>();
        for (SysYParser.BinaryExpContext dimension : ctx.binaryExp()) {
            VReg reg = visitBinaryExp(dimension);
            reg = typeConversion(reg, BasicType.I32);
            dimensions.add(reg);
        }
        curBlock.add(new LoadVIR(result, symbol, dimensions));
        return result;
    }

    @Override
    public VReg visitFuncCallExp(SysYParser.FuncCallExpContext ctx) {
        FuncSymbol symbol = symbolTable.getFunc(ctx.Ident().getSymbol().getText());
        List<VIRItem> params = new ArrayList<>();
        for (int i = 0; i < ctx.binaryExp().size(); i++) {
            SysYParser.BinaryExpContext exp = ctx.binaryExp().get(i);
            VReg param = visitBinaryExp(exp);
            Type targetType = symbol.getParams().get(i).isSingle() && symbol.getParams().get(i).getType() == BasicType.FLOAT ? BasicType.FLOAT : BasicType.I32;
            param = typeConversion(param, targetType);
            params.add(param);
        }
        VReg retReg = switch (symbol.getType()) {
            case BasicType.FLOAT -> new VReg(BasicType.FLOAT, 4);
            case BasicType.I32 -> new VReg(BasicType.I32, 4);
            case BasicType.VOID -> null;
            default -> throw new IllegalStateException("Unexpected value: " + symbol.getType());
        };
        curBlock.add(new CallVIR(symbol, retReg, params));
        return retReg;
    }

    @Override
    public VReg visitBinaryExp(SysYParser.BinaryExpContext ctx) {
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
            VReg lVal = visitBinaryExp(ctx.binaryExp(0));
            if (lVal != null) {
                VReg zero = new VReg(lVal.getType(), lVal.getSize());
                curBlock.add(new LiVIR(zero, 0));
                curBlock.add(new BranchVIR(BranchVIR.Type.NE, lVal, zero, this.trueBlock, this.falseBlock));
            }
            this.curBlock = rBlock;
            this.trueBlock = trueBlock;
            this.falseBlock = falseBlock;
            VReg rVal = visitBinaryExp(ctx.binaryExp(1));
            if (rVal != null) {
                VReg zero = new VReg(rVal.getType(), rVal.getSize());
                curBlock.add(new LiVIR(zero, 0));
                curBlock.add(new BranchVIR(BranchVIR.Type.NE, rVal, zero, this.trueBlock, this.falseBlock));
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
            VReg lVal = visitBinaryExp(ctx.binaryExp(0));
            if (lVal != null) {
                VReg zero = new VReg(lVal.getType(), lVal.getSize());
                curBlock.add(new LiVIR(zero, 0));
                curBlock.add(new BranchVIR(BranchVIR.Type.NE, lVal, zero, this.trueBlock, this.falseBlock));
            }
            this.curBlock = rBlock;
            this.trueBlock = trueBlock;
            this.falseBlock = falseBlock;
            VReg rVal = visitBinaryExp(ctx.binaryExp(1));
            if (rVal != null) {
                VReg zero = new VReg(rVal.getType(), rVal.getSize());
                curBlock.add(new LiVIR(zero, 0));
                curBlock.add(new BranchVIR(BranchVIR.Type.NE, rVal, zero, this.trueBlock, this.falseBlock));
            }
            return null;
        }
        boolean calculatingCond = this.calculatingCond;
        this.calculatingCond = false;
        VReg lReg = visitBinaryExp(ctx.binaryExp(0));
        VReg rReg = visitBinaryExp(ctx.binaryExp(1));
        Type targetType = automaticTypePromotion(lReg.getType(), rReg.getType());
        VReg result = new VReg(targetType, Integer.max(lReg.getSize(), rReg.getSize()));
        lReg = typeConversion(lReg, targetType);
        rReg = typeConversion(rReg, targetType);
        String op = ctx.getChild(1).getText();
        switch (op) {
            case "+", "-", "*", "/", "%" -> curBlock.add(new BinaryVIR(switch (op) {
                case "+" -> BinaryVIR.Type.ADD;
                case "-" -> BinaryVIR.Type.SUB;
                case "*" -> BinaryVIR.Type.MUL;
                case "/" -> BinaryVIR.Type.DIV;
                case "%" -> BinaryVIR.Type.MOD;
                default -> throw new IllegalStateException("Unexpected value: " + op);
            }, result, lReg, rReg));
            case "==", "!=", ">=", ">", "<=", "<" -> {
                result = new VReg(BasicType.I32, 4);
                curBlock.add(calculatingCond ? new BranchVIR(switch (op) {
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
                }, result, lReg, rReg));
            }
            default -> throw new IllegalStateException("Unexpected value: " + ctx.getChild(1).getText());
        }
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
            DataSymbol symbol = symbolTable.getData(name);
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

    private VReg typeConversion(VReg reg, Type targetType) {
        if (reg.getType() == targetType)
            return reg;
        if (reg.getType() == BasicType.FLOAT && targetType == BasicType.I32) {
            VReg newReg = new VReg(BasicType.I32, 4);
            curBlock.add(new UnaryVIR(UnaryVIR.Type.F2I, newReg, reg));
            reg = newReg;
        }
        if (reg.getType() == BasicType.I32 && targetType == BasicType.FLOAT) {
            VReg newReg = new VReg(BasicType.FLOAT, 4);
            curBlock.add(new UnaryVIR(UnaryVIR.Type.I2F, newReg, reg));
            reg = newReg;
        }
        return reg;
    }
}
