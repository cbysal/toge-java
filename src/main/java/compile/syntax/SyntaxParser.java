package compile.syntax;

import compile.lexical.token.Token;
import compile.lexical.token.TokenList;
import compile.symbol.*;
import compile.syntax.ast.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SyntaxParser {
    private boolean isProcessed = false;
    private final SymbolTable symbolTable;
    private final TokenList tokens;

    private final RootAST rootAST = new RootAST();

    public SyntaxParser(SymbolTable symbolTable, TokenList tokens) {
        this.symbolTable = symbolTable;
        this.tokens = tokens;
    }

    private void checkIfIsProcessed() {
        if (isProcessed) {
            return;
        }
        isProcessed = true;
        parseRoot();
    }

    private void parseRoot() {
        while (tokens.hasNext()) {
            switch (tokens.peekType()) {
                case CONST -> rootAST.addAll(parseConstDef());
                case FLOAT, INT, VOID -> {
                    if (tokens.peekType(2) == Token.Type.LP) {
                        rootAST.add(parseFuncDef());
                    } else {
                        rootAST.addAll(parseGlobalDef());
                    }
                }
                default -> throw new RuntimeException();
            }
        }
    }

    private List<ConstDefAST> parseConstDef() {
        List<ConstDefAST> constDef = new ArrayList<>();
        tokens.expectAndFetch(Token.Type.CONST);
        boolean isFloat = switch (tokens.expectAndFetch(Token.Type.FLOAT, Token.Type.INT).getType()) {
            case FLOAT -> true;
            case INT -> false;
            default -> throw new RuntimeException();
        };
        do {
            String name = tokens.nextIdentity();
            if (tokens.match(Token.Type.LB)) {
                List<Integer> dimensions = parseDimensionDef();
                tokens.expectAndFetch(Token.Type.ASSIGN);
                InitValAST initVal = parseInitVal();
                Map<Integer, ExpAST> exps = new HashMap<>();
                allocInitVal(dimensions, exps, 0, initVal);
                if (isFloat) {
                    constDef.add(new ConstDefAST(symbolTable.makeConst(true, name, dimensions,
                            exps.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                                    exp -> Float.floatToIntBits(exp.getValue().calc().getFloat()))))));
                } else {
                    constDef.add(new ConstDefAST(symbolTable.makeConst(false, name, dimensions,
                            exps.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                                    exp -> exp.getValue().calc().getInt())))));
                }
            } else {
                tokens.expectAndFetch(Token.Type.ASSIGN);
                ExpAST rVal = parseAddSubExp();
                Value value = rVal.calc();
                if (isFloat) {
                    constDef.add(new ConstDefAST(symbolTable.makeConst(true, name, value.isFloat() ?
                            value.getFloat() : value.getInt())));
                } else {
                    constDef.add(new ConstDefAST(symbolTable.makeConst(false, name, value.isFloat() ?
                            (int) value.getFloat() : value.getInt())));
                }
            }
            tokens.matchAndThenThrow(Token.Type.COMMA);
        } while (tokens.peekType() != Token.Type.SEMICOLON);
        tokens.expectAndFetch(Token.Type.SEMICOLON);
        return constDef;
    }

    private List<GlobalDefAST> parseGlobalDef() {
        boolean isFloat = switch (tokens.expectAndFetch(Token.Type.FLOAT, Token.Type.INT).getType()) {
            case FLOAT -> true;
            case INT -> false;
            default -> throw new RuntimeException();
        };
        List<GlobalDefAST> globalDefs = new ArrayList<>();
        do {
            String name = tokens.nextIdentity();
            if (tokens.match(Token.Type.LB)) {
                List<Integer> dimensions = parseDimensionDef();
                if (tokens.matchAndThenThrow(Token.Type.ASSIGN)) {
                    InitValAST initVal = parseInitVal();
                    Map<Integer, ExpAST> exps = new HashMap<>();
                    allocInitVal(dimensions, exps, 0, initVal);
                    if (isFloat) {
                        globalDefs.add(new GlobalDefAST(symbolTable.makeGlobal(true, name, dimensions,
                                exps.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                                        exp -> Float.floatToIntBits(exp.getValue().calc().getFloat()))))));
                    } else {
                        globalDefs.add(new GlobalDefAST(symbolTable.makeGlobal(false, name, dimensions,
                                exps.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                                        exp -> exp.getValue().calc().getInt())))));
                    }
                } else {
                    globalDefs.add(new GlobalDefAST(symbolTable.makeGlobal(isFloat, name, dimensions,
                            new HashMap<>())));
                }
            } else {
                if (tokens.matchAndThenThrow(Token.Type.ASSIGN)) {
                    ExpAST rVal = parseAddSubExp();
                    if (isFloat) {
                        globalDefs.add(new GlobalDefAST(symbolTable.makeGlobal(true, name, rVal.calc().getFloat())));
                    } else {
                        globalDefs.add(new GlobalDefAST(symbolTable.makeGlobal(false, name, rVal.calc().getInt())));
                    }
                } else {
                    globalDefs.add(new GlobalDefAST(symbolTable.makeGlobal(isFloat, name, 0)));
                }
            }
            tokens.matchAndThenThrow(Token.Type.COMMA);
        } while (!tokens.matchAndThenThrow(Token.Type.SEMICOLON));
        return globalDefs;
    }

    private List<StmtAST> parseLocalDef() {
        boolean isFloat = switch (tokens.expectAndFetch(Token.Type.FLOAT, Token.Type.INT).getType()) {
            case FLOAT -> true;
            case INT -> false;
            default -> throw new RuntimeException();
        };
        List<StmtAST> stmts = new ArrayList<>();
        do {
            String name = tokens.nextIdentity();
            if (tokens.match(Token.Type.LB)) {
                List<Integer> dimensions = parseDimensionDef();
                LocalSymbol symbol = symbolTable.makeLocal(isFloat, name, dimensions);
                stmts.add(new LocalDefAST(symbol));
                if (tokens.matchAndThenThrow(Token.Type.ASSIGN)) {
                    stmts.add(new ExpStmtAST(new FuncCallExpAST(symbolTable.getFunc("memset"),
                            List.of(new VarExpAST(symbol), new IntLitExpAST(0),
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
                LocalSymbol symbol = symbolTable.makeLocal(isFloat, name);
                stmts.add(new LocalDefAST(symbol));
                if (tokens.matchAndThenThrow(Token.Type.ASSIGN)) {
                    LValAST lVal = new LValAST(symbol);
                    ExpAST rVal = parseAddSubExp();
                    stmts.add(new AssignStmtAST(lVal, rVal));
                }
            }
        } while (tokens.matchAndThenThrow(Token.Type.COMMA));
        tokens.expectAndFetch(Token.Type.SEMICOLON);
        return stmts;
    }

    private List<Integer> parseDimensionDef() {
        List<Integer> dimensions = new ArrayList<>();
        while (tokens.matchAndThenThrow(Token.Type.LB)) {
            dimensions.add(parseAddSubExp().calc().getInt());
            tokens.expectAndFetch(Token.Type.RB);
        }
        return dimensions;
    }

    private CompUnitAST parseFuncDef() {
        boolean hasRet, isFloat;
        switch (tokens.expectAndFetch(Token.Type.FLOAT, Token.Type.INT, Token.Type.VOID).getType()) {
            case FLOAT -> {
                hasRet = true;
                isFloat = true;
            }
            case INT -> {
                hasRet = true;
                isFloat = false;
            }
            case VOID -> {
                hasRet = false;
                isFloat = false;
            }
            default -> throw new RuntimeException();
        }
        String name = tokens.nextIdentity();
        FuncSymbol decl = hasRet ? symbolTable.makeFunc(isFloat, name) : symbolTable.makeFunc(name);
        symbolTable.in();
        parseFuncDefParams(decl);
        BlockStmtAST body = parseBlock();
        symbolTable.out();
        return new FuncDefAST(decl, body);
    }

    private void parseFuncDefParams(FuncSymbol func) {
        tokens.expectAndFetch(Token.Type.LP);
        while (!tokens.matchAndThenThrow(Token.Type.RP)) {
            func.addParam(parseFuncDefParam());
            if (tokens.matchAndThenThrow(Token.Type.RP)) {
                break;
            }
            tokens.expectAndFetch(Token.Type.COMMA);
        }
    }

    private ParamSymbol parseFuncDefParam() {
        boolean isFloat = switch (tokens.expectAndFetch(Token.Type.FLOAT, Token.Type.INT).getType()) {
            case FLOAT -> true;
            case INT -> false;
            default -> throw new RuntimeException();
        };
        String name = tokens.nextIdentity();
        if (!tokens.matchAndThenThrow(Token.Type.LB)) {
            return symbolTable.makeParam(isFloat, name);
        }
        tokens.expectAndFetch(Token.Type.RB);
        List<Integer> dimensions = new ArrayList<>();
        dimensions.add(-1);
        while (tokens.matchAndThenThrow(Token.Type.LB)) {
            ExpAST exp = parseAddSubExp();
            dimensions.add(exp.calc().getInt());
            tokens.matchAndThenThrow(Token.Type.RB);
        }
        return symbolTable.makeParam(isFloat, name, dimensions);
    }

    private BlockStmtAST parseBlock() {
        BlockStmtAST blockStmt = new BlockStmtAST();
        tokens.expectAndFetch(Token.Type.LC);
        while (!tokens.matchAndThenThrow(Token.Type.RC)) {
            blockStmt.addAll(parseStmt());
        }
        return blockStmt;
    }

    private List<? extends StmtAST> parseStmt() {
        return switch (tokens.peekType()) {
            case BREAK -> List.of(parseBreakStmt());
            case CONST -> parseConstDef();
            case CONTINUE -> List.of(parseContinueStmt());
            case FLOAT, INT -> parseLocalDef();
            case ID -> {
                int lookahead = 0;
                boolean isAssignStmt = false;
                while (tokens.peekType(lookahead) != Token.Type.SEMICOLON) {
                    if (tokens.peekType(lookahead) == Token.Type.ASSIGN) {
                        isAssignStmt = true;
                        break;
                    }
                    lookahead++;
                }
                if (isAssignStmt) {
                    yield List.of(parseAssignStmt());
                } else {
                    yield List.of(parseExpStmt());
                }
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

    private ExpAST parseLOrExp() {
        List<ExpAST> exps = new ArrayList<>();
        exps.add(parseLAndExp());
        while (tokens.matchAndThenThrow(Token.Type.L_OR)) {
            exps.add(parseLAndExp());
        }
        ExpAST root = exps.get(0);
        for (int i = 1; i < exps.size(); i++) {
            root = new BinaryExpAST(BinaryExpAST.Type.L_OR, root, exps.get(i));
        }
        return root;
    }

    private ExpAST parseLAndExp() {
        List<ExpAST> exps = new ArrayList<>();
        exps.add(parseEqNeExp());
        while (tokens.matchAndThenThrow(Token.Type.L_AND)) {
            exps.add(parseEqNeExp());
        }
        ExpAST root = exps.get(0);
        for (int i = 1; i < exps.size(); i++) {
            root = new BinaryExpAST(BinaryExpAST.Type.L_AND, root, exps.get(i));
        }
        return root;
    }

    private ExpAST parseEqNeExp() {
        List<BinaryExpAST.Type> types = new ArrayList<>();
        List<ExpAST> exps = new ArrayList<>();
        exps.add(parseGeGtLeLtExp());
        while (tokens.match(Token.Type.EQ, Token.Type.NE)) {
            types.add(switch (tokens.expectAndFetch(Token.Type.EQ, Token.Type.NE).getType()) {
                case EQ -> BinaryExpAST.Type.EQ;
                case NE -> BinaryExpAST.Type.NE;
                default -> throw new RuntimeException();
            });
            exps.add(parseGeGtLeLtExp());
        }
        ExpAST root = exps.get(0);
        for (int i = 0; i < types.size(); i++) {
            root = new BinaryExpAST(types.get(i), root, exps.get(i + 1));
        }
        return root;
    }

    private ExpAST parseGeGtLeLtExp() {
        List<BinaryExpAST.Type> types = new ArrayList<>();
        List<ExpAST> exps = new ArrayList<>();
        exps.add(parseAddSubExp());
        while (tokens.match(Token.Type.GE, Token.Type.GT, Token.Type.LE, Token.Type.LT)) {
            types.add(switch (tokens.expectAndFetch(Token.Type.GE, Token.Type.GT, Token.Type.LE, Token.Type.LT).getType()) {
                case GE -> BinaryExpAST.Type.GE;
                case GT -> BinaryExpAST.Type.GT;
                case LE -> BinaryExpAST.Type.LE;
                case LT -> BinaryExpAST.Type.LT;
                default -> throw new RuntimeException();
            });
            exps.add(parseAddSubExp());
        }
        ExpAST root = exps.get(0);
        for (int i = 0; i < types.size(); i++) {
            root = new BinaryExpAST(types.get(i), root, exps.get(i + 1));
        }
        return root;
    }

    private ExpAST parseAddSubExp() {
        List<BinaryExpAST.Type> types = new ArrayList<>();
        List<ExpAST> exps = new ArrayList<>();
        exps.add(parseMulDivModExp());
        while (tokens.match(Token.Type.PLUS, Token.Type.MINUS)) {
            types.add(switch (tokens.expectAndFetch(Token.Type.PLUS, Token.Type.MINUS).getType()) {
                case PLUS -> BinaryExpAST.Type.ADD;
                case MINUS -> BinaryExpAST.Type.SUB;
                default -> throw new RuntimeException();
            });
            exps.add(parseMulDivModExp());
        }
        ExpAST root = exps.get(0);
        for (int i = 0; i < types.size(); i++) {
            root = new BinaryExpAST(types.get(i), root, exps.get(i + 1));
        }
        return root;
    }

    private ExpAST parseMulDivModExp() {
        List<BinaryExpAST.Type> types = new ArrayList<>();
        List<ExpAST> exps = new ArrayList<>();
        exps.add(parseUnary());
        while (tokens.match(Token.Type.MUL, Token.Type.DIV, Token.Type.MOD)) {
            types.add(switch (tokens.expectAndFetch(Token.Type.MUL, Token.Type.DIV, Token.Type.MOD).getType()) {
                case MUL -> BinaryExpAST.Type.MUL;
                case DIV -> BinaryExpAST.Type.DIV;
                case MOD -> BinaryExpAST.Type.MOD;
                default -> throw new RuntimeException();
            });
            exps.add(parseUnary());
        }
        ExpAST root = exps.get(0);
        for (int i = 0; i < types.size(); i++) {
            root = new BinaryExpAST(types.get(i), root, exps.get(i + 1));
        }
        return root;
    }

    private ExpAST parseUnary() {
        return switch (tokens.peekType()) {
            case FLOAT_LIT -> new FloatLitExpAST(tokens.nextFloat());
            case ID -> {
                if (tokens.peekType(1) == Token.Type.LP) {
                    yield parseFuncCallExp();
                } else {
                    yield parseVarExpAST();
                }
            }
            case INT_LIT -> new IntLitExpAST(tokens.nextInt());
            case L_NOT -> {
                tokens.expectAndFetch(Token.Type.L_NOT);
                yield new UnaryExpAST(UnaryExpAST.Type.L_NOT, parseUnary());
            }
            case LP -> {
                tokens.expectAndFetch(Token.Type.LP);
                ExpAST exp = parseAddSubExp();
                tokens.expectAndFetch(Token.Type.RP);
                yield exp;
            }
            case MINUS -> {
                tokens.expectAndFetch(Token.Type.MINUS);
                yield new UnaryExpAST(UnaryExpAST.Type.NEG, parseUnary());
            }
            case PLUS -> {
                tokens.expectAndFetch(Token.Type.PLUS);
                yield parseUnary();
            }
            default -> throw new RuntimeException();
        };
    }

    private StmtAST parseAssignStmt() {
        LValAST lVal = parseLVal();
        tokens.expectAndFetch(Token.Type.ASSIGN);
        ExpAST rVal = parseAddSubExp();
        tokens.expectAndFetch(Token.Type.SEMICOLON);
        return new AssignStmtAST(lVal, rVal);
    }

    private BlankStmtAST parseBlankStmt() {
        tokens.expectAndFetch(Token.Type.SEMICOLON);
        return new BlankStmtAST();
    }

    private BreakStmtAST parseBreakStmt() {
        tokens.expectAndFetch(Token.Type.BREAK);
        tokens.expectAndFetch(Token.Type.SEMICOLON);
        return new BreakStmtAST();
    }

    private ContinueStmtAST parseContinueStmt() {
        tokens.expectAndFetch(Token.Type.CONTINUE);
        tokens.expectAndFetch(Token.Type.SEMICOLON);
        return new ContinueStmtAST();
    }

    private ExpAST parseCond() {
        tokens.expectAndFetch(Token.Type.LP);
        ExpAST cond = parseLOrExp();
        tokens.expectAndFetch(Token.Type.RP);
        return cond;
    }

    private List<ExpAST> parseDimensionExp() {
        List<ExpAST> dimensions = new ArrayList<>();
        while (tokens.matchAndThenThrow(Token.Type.LB)) {
            dimensions.add(parseAddSubExp());
            tokens.expectAndFetch(Token.Type.RB);
        }
        return dimensions;
    }

    private ExpStmtAST parseExpStmt() {
        ExpStmtAST expStmt = new ExpStmtAST(parseAddSubExp());
        tokens.expectAndFetch(Token.Type.SEMICOLON);
        return expStmt;
    }

    private FuncCallExpAST parseFuncCallExp() {
        FuncSymbol func = symbolTable.getFunc(tokens.nextIdentity());
        tokens.expectAndFetch(Token.Type.LP);
        List<ExpAST> params = new ArrayList<>();
        while (!tokens.matchAndThenThrow(Token.Type.RP)) {
            params.add(parseAddSubExp());
            if (tokens.matchAndThenThrow(Token.Type.RP)) {
                break;
            }
            tokens.expectAndFetch(Token.Type.COMMA);
        }
        return new FuncCallExpAST(func, params);
    }

    private IfStmtAST parseIfStmt() {
        tokens.expectAndFetch(Token.Type.IF);
        ExpAST cond = parseCond();
        List<? extends StmtAST> stmts = parseStmt();
        if (stmts.size() != 1) {
            throw new RuntimeException();
        }
        StmtAST stmt1 = stmts.get(0);
        if (!tokens.matchAndThenThrow(Token.Type.ELSE)) {
            return new IfStmtAST(cond, stmt1);
        }
        stmts = parseStmt();
        if (stmts.size() != 1) {
            throw new RuntimeException();
        }
        StmtAST stmt2 = stmts.get(0);
        return new IfStmtAST(cond, stmt1, stmt2);
    }

    private InitValAST parseInitVal() {
        tokens.expectAndFetch(Token.Type.LC);
        InitValAST initVal = new InitValAST();
        if (tokens.matchAndThenThrow(Token.Type.RC)) {
            return initVal;
        }
        do {
            if (tokens.match(Token.Type.LC)) {
                initVal.add(parseInitVal());
            } else {
                initVal.add(parseAddSubExp());
            }
        } while (tokens.matchAndThenThrow(Token.Type.COMMA));
        tokens.expectAndFetch(Token.Type.RC);
        return initVal;
    }

    private LValAST parseLVal() {
        String name = tokens.nextIdentity();
        if (tokens.match(Token.Type.LB)) {
            List<ExpAST> dimensions = parseDimensionExp();
            return new LValAST(symbolTable.getData(name), dimensions);
        }
        return new LValAST(symbolTable.getData(name));
    }

    private StmtAST parseRetStmt() {
        tokens.expectAndFetch(Token.Type.RETURN);
        if (tokens.matchAndThenThrow(Token.Type.SEMICOLON)) {
            return new RetStmtAST();
        }
        ExpAST retVal = parseAddSubExp();
        tokens.expectAndFetch(Token.Type.SEMICOLON);
        return new RetStmtAST(retVal);
    }

    private ExpAST parseVarExpAST() {
        String name = tokens.nextIdentity();
        if (tokens.match(Token.Type.LB)) {
            List<ExpAST> dimensions = parseDimensionExp();
            return new VarExpAST(symbolTable.getData(name), dimensions);
        }
        return new VarExpAST(symbolTable.getData(name));
    }

    private WhileStmtAST parseWhileStmt() {
        tokens.expectAndFetch(Token.Type.WHILE);
        ExpAST cond = parseCond();
        List<? extends StmtAST> stmts = parseStmt();
        if (stmts.size() != 1) {
            throw new RuntimeException();
        }
        StmtAST body = stmts.get(0);
        return new WhileStmtAST(cond, body);
    }

    private void allocInitVal(List<Integer> dimensions, Map<Integer, ExpAST> exps, int base, ExpAST src) {
        if (dimensions.isEmpty()) {
            while (src instanceof InitValAST initVal) {
                src = initVal.isEmpty() ? null : initVal.get(0);
            }
            if (src != null) {
                exps.put(base, src);
            }
            return;
        }
        int[] index = new int[dimensions.size()];
        for (ExpAST exp : (InitValAST) src) {
            if (exp instanceof InitValAST) {
                int d = Integer.max(dimensions.lastIndexOf(0), 0);
                int offset = 0;
                for (int i = 0; i < dimensions.size(); i++) {
                    offset = offset * dimensions.get(i) + (i <= d ? index[i] : 0);
                }
                allocInitVal(dimensions.subList(d + 1, dimensions.size()), exps, base + offset, exp);
                index[d]++;
            } else {
                int offset = 0;
                for (int i = 0; i < dimensions.size(); i++) {
                    offset = offset * dimensions.get(i) + index[i];
                }
                exps.put(base + offset, exp);
                index[index.length - 1]++;
            }
            for (int i = dimensions.size() - 1; i >= 0 && index[i] >= dimensions.get(i); i--) {
                index[i] = 0;
                if (i == 0) {
                    return;
                }
                index[i - 1]++;
            }
        }
    }

    public RootAST getRootAST() {
        checkIfIsProcessed();
        return rootAST;
    }
}
