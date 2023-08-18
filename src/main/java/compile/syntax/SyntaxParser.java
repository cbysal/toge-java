package compile.syntax;

import compile.lexical.token.TokenList;
import compile.lexical.token.TokenType;
import compile.symbol.*;
import compile.syntax.ast.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SyntaxParser {
    private final SymbolTable symbolTable;
    private final TokenList tokens;

    private RootAST rootAST;

    public SyntaxParser(SymbolTable symbolTable, TokenList tokens) {
        this.symbolTable = symbolTable;
        this.tokens = tokens;
    }

    private void allocInitVal(List<Integer> dimensions, Map<Integer, ExpAST> exps, int base, ExpAST src) {
        if (dimensions.isEmpty()) {
            while (src instanceof InitValAST initVal)
                src = initVal.isEmpty() ? null : initVal.get(0);
            if (src != null)
                exps.put(base, src);
            return;
        }
        int[] index = new int[dimensions.size()];
        for (ExpAST exp : (InitValAST) src) {
            if (exp instanceof InitValAST) {
                int d = Integer.max(dimensions.lastIndexOf(0), 0);
                int offset = 0;
                for (int i = 0; i < dimensions.size(); i++)
                    offset = offset * dimensions.get(i) + (i <= d ? index[i] : 0);
                allocInitVal(dimensions.subList(d + 1, dimensions.size()), exps, base + offset, exp);
                index[d]++;
            } else {
                int offset = 0;
                for (int i = 0; i < dimensions.size(); i++)
                    offset = offset * dimensions.get(i) + index[i];
                exps.put(base + offset, exp);
                index[index.length - 1]++;
            }
            for (int i = dimensions.size() - 1; i >= 0 && index[i] >= dimensions.get(i); i--) {
                index[i] = 0;
                if (i == 0)
                    return;
                index[i - 1]++;
            }
        }
    }

    public RootAST getRootAST() {
        parseRoot();
        return rootAST;
    }

    private ExpAST parseAddSubExp() {
        List<BinaryExpAST.Op> ops = new ArrayList<>();
        List<ExpAST> exps = new ArrayList<>();
        exps.add(parseMulDivModExp());
        while (tokens.expect(TokenType.PLUS, TokenType.MINUS)) {
            ops.add(switch (tokens.expectAndFetch(TokenType.PLUS, TokenType.MINUS).getType()) {
                case PLUS -> BinaryExpAST.Op.ADD;
                case MINUS -> BinaryExpAST.Op.SUB;
                default -> throw new RuntimeException();
            });
            exps.add(parseMulDivModExp());
        }
        ExpAST root = exps.get(0);
        for (int i = 0; i < ops.size(); i++) {
            root = new BinaryExpAST(ops.get(i), root, exps.get(i + 1));
        }
        return root;
    }

    private StmtAST parseAssignStmt() {
        LValAST lVal = parseLVal();
        tokens.next(TokenType.ASSIGN);
        ExpAST rVal = parseAddSubExp();
        tokens.next(TokenType.SEMICOLON);
        return new AssignStmtAST(lVal, rVal);
    }

    private BlankStmt parseBlankStmt() {
        tokens.next(TokenType.SEMICOLON);
        return new BlankStmt();
    }

    private BlockStmtAST parseBlock() {
        BlockStmtAST blockStmt = new BlockStmtAST();
        tokens.next(TokenType.LC);
        while (!tokens.expect(TokenType.RC))
            blockStmt.addAll(parseStmt());
        tokens.next(TokenType.RC);
        return blockStmt;
    }

    private BreakStmtAST parseBreakStmt() {
        tokens.next(TokenType.BREAK);
        tokens.next(TokenType.SEMICOLON);
        return new BreakStmtAST();
    }

    private ExpAST parseCond() {
        tokens.next(TokenType.LP);
        ExpAST cond = parseLOrExp();
        tokens.next(TokenType.RP);
        return cond;
    }

    private List<ConstDefAST> parseConstDef() {
        List<ConstDefAST> condDef = new ArrayList<>();
        tokens.next(TokenType.CONST);
        boolean isFloat = switch (tokens.next(TokenType.FLOAT, TokenType.INT)) {
            case FLOAT -> true;
            case INT -> false;
            default -> throw new RuntimeException();
        };
        do {
            String name = tokens.nextIdentity();
            if (tokens.expect(TokenType.LB)) {
                List<Integer> dimensions = parseDimensionDef();
                tokens.next(TokenType.ASSIGN);
                InitValAST initVal = parseInitVal();
                Map<Integer, ExpAST> exps = new HashMap<>();
                allocInitVal(dimensions, exps, 0, initVal);
                if (isFloat)
                    condDef.add(new ConstDefAST(symbolTable.makeConst(Type.FLOAT, name, dimensions,
                            exps.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                                    exp -> Float.floatToIntBits(exp.getValue().calc().getFloat()))))));
                else
                    condDef.add(new ConstDefAST(symbolTable.makeConst(Type.INT, name, dimensions,
                            exps.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                                    exp -> exp.getValue().calc().getInt())))));
            } else {
                tokens.next(TokenType.ASSIGN);
                ExpAST rVal = parseAddSubExp();
                Value value = rVal.calc();
                if (isFloat)
                    condDef.add(new ConstDefAST(symbolTable.makeConst(Type.FLOAT, name,
                            value.getType() == Type.FLOAT ? value.getFloat() : value.getInt())));
                else
                    condDef.add(new ConstDefAST(symbolTable.makeConst(Type.INT, name, value.getType() == Type.FLOAT ?
                            (int) value.getFloat() : value.getInt())));
            }
            if (tokens.expect(TokenType.COMMA))
                tokens.next();
        } while (tokens.peek() != TokenType.SEMICOLON);
        tokens.next(TokenType.SEMICOLON);
        return condDef;
    }

    private ContinueStmtAST parseContinueStmt() {
        tokens.next(TokenType.CONTINUE);
        tokens.next(TokenType.SEMICOLON);
        return new ContinueStmtAST();
    }

    private List<Integer> parseDimensionDef() {
        List<Integer> dimensions = new ArrayList<>();
        while (tokens.expect(TokenType.LB)) {
            tokens.next();
            dimensions.add(parseAddSubExp().calc().getInt());
            tokens.next(TokenType.RB);
        }
        return dimensions;
    }

    private List<ExpAST> parseDimensionExp() {
        List<ExpAST> dimensions = new ArrayList<>();
        while (tokens.expect(TokenType.LB)) {
            tokens.next();
            dimensions.add(parseAddSubExp());
            tokens.next(TokenType.RB);
        }
        return dimensions;
    }

    private ExpAST parseEqNeExp() {
        ExpAST left = parseLeLtGeLtExp();
        while (tokens.expect(TokenType.EQ, TokenType.NE)) {
            CmpExpAST.Op op = switch (tokens.next(TokenType.EQ, TokenType.NE)) {
                case EQ -> CmpExpAST.Op.EQ;
                case NE -> CmpExpAST.Op.NE;
                default -> throw new RuntimeException();
            };
            ExpAST right = parseLeLtGeLtExp();
            left = new CmpExpAST(op, left, right);
        }
        return left;
    }

    private ExpStmtAST parseExpStmt() {
        ExpStmtAST expStmt = new ExpStmtAST(parseAddSubExp());
        tokens.next(TokenType.SEMICOLON);
        return expStmt;
    }

    private FuncCallExpAST parseFuncCallExp() {
        FuncSymbol func = symbolTable.getFunc(tokens.nextIdentity());
        tokens.next(TokenType.LP);
        List<ExpAST> params = new ArrayList<>();
        while (!tokens.expect(TokenType.RP)) {
            params.add(parseAddSubExp());
            if (tokens.expect(TokenType.RP))
                break;
            tokens.next(TokenType.COMMA);
        }
        tokens.next(TokenType.RP);
        return new FuncCallExpAST(func, params);
    }

    private CompUnitAST parseFuncDef() {
        Type retType = switch (tokens.next(TokenType.FLOAT, TokenType.INT, TokenType.VOID)) {
            case FLOAT -> Type.FLOAT;
            case INT -> Type.INT;
            case VOID -> Type.VOID;
            default -> throw new RuntimeException();
        };
        String name = tokens.nextIdentity();
        FuncSymbol decl = symbolTable.makeFunc(retType, name);
        symbolTable.in();
        parseFuncDefParams(decl);
        BlockStmtAST body = parseBlock();
        symbolTable.out();
        return new FuncDefAST(decl, body);
    }

    private ParamSymbol parseFuncDefParam() {
        Type type = switch (tokens.next(TokenType.FLOAT, TokenType.INT)) {
            case FLOAT -> Type.FLOAT;
            case INT -> Type.INT;
            default -> throw new RuntimeException();
        };
        String name = tokens.nextIdentity();
        if (!tokens.expect(TokenType.LB))
            return symbolTable.makeParam(type, name);
        tokens.next(TokenType.LB);
        tokens.next(TokenType.RB);
        List<Integer> dimensions = new ArrayList<>();
        dimensions.add(-1);
        while (tokens.expect(TokenType.LB)) {
            tokens.next();
            ExpAST exp = parseAddSubExp();
            dimensions.add(exp.calc().getInt());
            tokens.next(TokenType.RB);
        }
        return symbolTable.makeParam(type, name, dimensions);
    }

    private void parseFuncDefParams(FuncSymbol func) {
        tokens.next(TokenType.LP);
        while (!tokens.expect(TokenType.RP)) {
            func.addParam(parseFuncDefParam());
            if (tokens.expect(TokenType.RP))
                break;
            tokens.next(TokenType.COMMA);
        }
        tokens.next(TokenType.RP);
    }

    private List<GlobalDefAST> parseGlobalDef() {
        Type type = switch (tokens.next(TokenType.FLOAT, TokenType.INT)) {
            case FLOAT -> Type.FLOAT;
            case INT -> Type.INT;
            default -> throw new RuntimeException();
        };
        List<GlobalDefAST> globalDefs = new ArrayList<>();
        do {
            String name = tokens.nextIdentity();
            if (tokens.expect(TokenType.LB)) {
                List<Integer> dimensions = parseDimensionDef();
                if (tokens.expect(TokenType.ASSIGN)) {
                    tokens.next();
                    InitValAST initVal = parseInitVal();
                    Map<Integer, ExpAST> exps = new HashMap<>();
                    allocInitVal(dimensions, exps, 0, initVal);
                    if (type == Type.FLOAT) {
                        globalDefs.add(new GlobalDefAST(symbolTable.makeGlobal(type, name, dimensions,
                                exps.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                                        exp -> Float.floatToIntBits(exp.getValue().calc().getFloat()))))));
                    } else {
                        globalDefs.add(new GlobalDefAST(symbolTable.makeGlobal(type, name, dimensions,
                                exps.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                                        exp -> exp.getValue().calc().getInt())))));
                    }
                } else
                    globalDefs.add(new GlobalDefAST(symbolTable.makeGlobal(type, name, dimensions, new HashMap<>())));
            } else {
                if (tokens.expect(TokenType.ASSIGN)) {
                    tokens.next();
                    ExpAST rVal = parseAddSubExp();
                    if (type == Type.FLOAT)
                        globalDefs.add(new GlobalDefAST(symbolTable.makeGlobal(type, name, rVal.calc().getFloat())));
                    else
                        globalDefs.add(new GlobalDefAST(symbolTable.makeGlobal(type, name, rVal.calc().getInt())));
                } else
                    globalDefs.add(new GlobalDefAST(symbolTable.makeGlobal(type, name, 0)));
            }
            if (tokens.expect(TokenType.COMMA))
                tokens.next();
        } while (!tokens.expect(TokenType.SEMICOLON));
        tokens.next(TokenType.SEMICOLON);
        return globalDefs;
    }

    private IfStmtAST parseIfStmt() {
        tokens.next(TokenType.IF);
        ExpAST cond = parseCond();
        List<? extends StmtAST> stmts = parseStmt();
        if (stmts.size() != 1)
            throw new RuntimeException();
        StmtAST stmt1 = stmts.get(0);
        if (!tokens.expect(TokenType.ELSE))
            return new IfStmtAST(cond, stmt1, null);
        tokens.next();
        stmts = parseStmt();
        if (stmts.size() != 1)
            throw new RuntimeException();
        StmtAST stmt2 = stmts.get(0);
        return new IfStmtAST(cond, stmt1, stmt2);
    }

    private InitValAST parseInitVal() {
        tokens.next(TokenType.LC);
        InitValAST initVal = new InitValAST();
        while (!tokens.expect(TokenType.RC)) {
            if (tokens.expect(TokenType.LC))
                initVal.add(parseInitVal());
            else
                initVal.add(parseAddSubExp());
            if (tokens.expect(TokenType.RC))
                break;
            tokens.next(TokenType.COMMA);
        }
        tokens.next(TokenType.RC);
        return initVal;
    }

    private ExpAST parseLAndExp() {
        ExpAST left = parseEqNeExp();
        while (tokens.expect(TokenType.L_AND)) {
            tokens.next();
            ExpAST right = parseEqNeExp();
            left = new LAndExpAST(left, right);
        }
        return left;
    }

    private ExpAST parseLeLtGeLtExp() {
        ExpAST left = parseAddSubExp();
        while (tokens.expect(TokenType.GE, TokenType.GT, TokenType.LE, TokenType.LT)) {
            CmpExpAST.Op op = switch (tokens.next(TokenType.GE, TokenType.GT, TokenType.LE, TokenType.LT)) {
                case GE -> CmpExpAST.Op.GE;
                case GT -> CmpExpAST.Op.GT;
                case LE -> CmpExpAST.Op.LE;
                case LT -> CmpExpAST.Op.LT;
                default -> throw new RuntimeException();
            };
            ExpAST right = parseAddSubExp();
            left = new CmpExpAST(op, left, right);
        }
        return left;
    }

    private List<StmtAST> parseLocalDef() {
        Type type = switch (tokens.next(TokenType.FLOAT, TokenType.INT)) {
            case FLOAT -> Type.FLOAT;
            case INT -> Type.INT;
            default -> throw new RuntimeException();
        };
        List<StmtAST> stmts = new ArrayList<>();
        do {
            String name = tokens.nextIdentity();
            if (tokens.expect(TokenType.LB)) {
                List<Integer> dimensions = parseDimensionDef();
                LocalSymbol symbol = symbolTable.makeLocal(type, name, dimensions);
                stmts.add(new LocalDefAST(symbol));
                if (tokens.expect(TokenType.ASSIGN)) {
                    tokens.next();
                    stmts.add(new ExpStmtAST(new FuncCallExpAST(symbolTable.getFunc("memset"),
                            List.of(new VarExpAST(symbol, List.of()), new IntLitExpAST(0),
                                    new IntLitExpAST(dimensions.stream().reduce(4, (i1, i2) -> i1 * i2))))));
                    InitValAST initVal = parseInitVal();
                    Map<Integer, ExpAST> exps = new HashMap<>();
                    allocInitVal(dimensions, exps, 0, initVal);
                    for (Map.Entry<Integer, ExpAST> exp : exps.entrySet()) {
                        ExpAST[] dimensionExps = new ExpAST[dimensions.size()];
                        int t = exp.getKey();
                        for (int j = dimensions.size() - 1; j >= 0; j--) {
                            dimensionExps[j] = new IntLitExpAST(t % dimensions.get(j));
                            t /= dimensions.get(j);
                        }
                        stmts.add(new AssignStmtAST(new LValAST(symbol, List.of(dimensionExps)), exp.getValue()));
                    }
                }
            } else {
                LocalSymbol symbol = symbolTable.makeLocal(type, name);
                stmts.add(new LocalDefAST(symbol));
                LValAST lVal = new LValAST(symbol, List.of());
                if (tokens.expect(TokenType.ASSIGN)) {
                    tokens.next();
                    ExpAST rVal = parseAddSubExp();
                    stmts.add(new AssignStmtAST(lVal, rVal));
                }
            }
            if (tokens.expect(TokenType.COMMA))
                tokens.next();
        } while (!tokens.expect(TokenType.SEMICOLON));
        tokens.next(TokenType.SEMICOLON);
        return stmts;
    }

    private ExpAST parseLOrExp() {
        ExpAST left = parseLAndExp();
        while (tokens.expect(TokenType.L_OR)) {
            tokens.next();
            ExpAST right = parseLAndExp();
            left = new LOrExpAST(left, right);
        }
        return left;
    }

    private LValAST parseLVal() {
        String name = tokens.nextIdentity();
        if (tokens.expect(TokenType.LB)) {
            List<ExpAST> dimensions = parseDimensionExp();
            return new LValAST(symbolTable.getData(name), dimensions);
        }
        return new LValAST(symbolTable.getData(name), List.of());
    }

    private ExpAST parseMulDivModExp() {
        List<BinaryExpAST.Op> ops = new ArrayList<>();
        List<ExpAST> exps = new ArrayList<>();
        exps.add(parseUnary());
        while (tokens.expect(TokenType.MUL, TokenType.DIV, TokenType.MOD)) {
            ops.add(switch (tokens.expectAndFetch(TokenType.MUL, TokenType.DIV, TokenType.MOD).getType()) {
                case MUL -> BinaryExpAST.Op.MUL;
                case DIV -> BinaryExpAST.Op.DIV;
                case MOD -> BinaryExpAST.Op.MOD;
                default -> throw new RuntimeException();
            });
            exps.add(parseUnary());
        }
        ExpAST root = exps.get(0);
        for (int i = 0; i < ops.size(); i++) {
            root = new BinaryExpAST(ops.get(i), root, exps.get(i + 1));
        }
        return root;
    }

    private void parseRoot() {
        rootAST = new RootAST();
        while (tokens.hasNext()) {
            switch (tokens.peek(TokenType.CONST, TokenType.FLOAT, TokenType.INT, TokenType.VOID)) {
                case CONST -> rootAST.addAll(parseConstDef());
                case FLOAT, INT, VOID -> {
                    if (tokens.expect(2, TokenType.LP))
                        rootAST.add(parseFuncDef());
                    else
                        rootAST.addAll(parseGlobalDef());
                }
                default -> throw new RuntimeException();
            }
        }
    }

    private StmtAST parseRetStmt() {
        tokens.next(TokenType.RETURN);
        if (tokens.expect(TokenType.SEMICOLON)) {
            tokens.next();
            return new RetStmtAST(null);
        }
        ExpAST retVal = parseAddSubExp();
        tokens.next(TokenType.SEMICOLON);
        return new RetStmtAST(retVal);
    }

    private List<? extends StmtAST> parseStmt() {
        return switch (tokens.peek()) {
            case BREAK -> List.of(parseBreakStmt());
            case CONST -> parseConstDef();
            case CONTINUE -> List.of(parseContinueStmt());
            case FLOAT, INT -> parseLocalDef();
            case ID -> {
                int lookahead = 0;
                boolean isAssignStmt = false;
                while (tokens.peek(lookahead) != TokenType.SEMICOLON) {
                    if (tokens.peek(lookahead) == TokenType.ASSIGN) {
                        isAssignStmt = true;
                        break;
                    }
                    lookahead++;
                }
                if (isAssignStmt)
                    yield List.of(parseAssignStmt());
                else
                    yield List.of(parseExpStmt());
            }
            case IF -> {
                symbolTable.in();
                IfStmtAST ifStmt = parseIfStmt();
                symbolTable.out();
                yield List.of(ifStmt);
            }
            case LC -> {
                symbolTable.in();
                BlockStmtAST blockStmt = parseBlock();
                symbolTable.out();
                yield List.of(blockStmt);
            }
            case RETURN -> List.of(parseRetStmt());
            case SEMICOLON -> List.of(parseBlankStmt());
            case WHILE -> {
                symbolTable.in();
                WhileStmtAST whileStmt = parseWhileStmt();
                symbolTable.out();
                yield List.of(whileStmt);
            }
            default -> throw new RuntimeException();
        };
    }

    private ExpAST parseUnary() {
        return switch (tokens.peek()) {
            case FLOAT_LIT -> new FloatLitExpAST(tokens.nextFloat());
            case ID -> {
                if (tokens.peek(1) == TokenType.LP)
                    yield parseFuncCallExp();
                else
                    yield parseVarExpAST();
            }
            case INT_LIT -> new IntLitExpAST(tokens.nextInt());
            case L_NOT -> {
                tokens.next();
                yield new LNotExpAST(parseUnary());
            }
            case LP -> {
                tokens.next();
                ExpAST exp = parseAddSubExp();
                tokens.next(TokenType.RP);
                yield exp;
            }
            case MINUS -> {
                tokens.next();
                yield new UnaryExpAST(UnaryExpAST.Op.NEG, parseUnary());
            }
            case PLUS -> {
                tokens.next();
                yield parseUnary();
            }
            default -> throw new RuntimeException();
        };
    }

    private ExpAST parseVarExpAST() {
        String name = tokens.nextIdentity();
        if (tokens.expect(TokenType.LB)) {
            List<ExpAST> dimensions = parseDimensionExp();
            return new VarExpAST(symbolTable.getData(name), dimensions);
        }
        return new VarExpAST(symbolTable.getData(name), List.of());
    }

    private WhileStmtAST parseWhileStmt() {
        tokens.next(TokenType.WHILE);
        ExpAST cond = parseCond();
        List<? extends StmtAST> stmts = parseStmt();
        if (stmts.size() != 1)
            throw new RuntimeException();
        StmtAST body = stmts.get(0);
        return new WhileStmtAST(cond, body);
    }
}
