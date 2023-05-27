package compile.syntax;

import compile.lexical.token.TokenList;
import compile.lexical.token.TokenType;
import compile.llvm.ir.type.ArrayType;
import compile.llvm.ir.type.BasicType;
import compile.llvm.ir.type.PointerType;
import compile.llvm.ir.type.Type;
import compile.symbol.FuncSymbol;
import compile.symbol.LocalSymbol;
import compile.symbol.ParamSymbol;
import compile.symbol.SymbolTable;
import compile.syntax.ast.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SyntaxParser {
    private static final Map<TokenType, Function<SyntaxParser, List<? extends StmtAST>>> PARSE_STMT_ACTIONS;
    private static final Map<TokenType, Function<SyntaxParser, ExpAST>> PARSE_UNARY_ACTIONS;

    static {
        Map<TokenType, Function<SyntaxParser, List<? extends StmtAST>>> parseStmtActions = new HashMap<>();
        parseStmtActions.put(TokenType.BREAK, parser -> List.of(parser.parseBreakStmt()));
        parseStmtActions.put(TokenType.CONST, SyntaxParser::parseConstDecl);
        parseStmtActions.put(TokenType.CONTINUE, parser -> List.of(parser.parseContinueStmt()));
        parseStmtActions.put(TokenType.FLOAT, SyntaxParser::parseLocalDecl);
        parseStmtActions.put(TokenType.ID, parser -> {
            if (parser.testAssignStmt()) {
                return List.of(parser.parseAssignStmt());
            } else {
                return List.of(parser.parseExpStmt());
            }
        });
        parseStmtActions.put(TokenType.IF, parser -> List.of(parser.parseIfStmt()));
        parseStmtActions.put(TokenType.INT, SyntaxParser::parseLocalDecl);
        parseStmtActions.put(TokenType.LC, parser -> {
            parser.symbolTable.in();
            BlockStmtAST blockStmt = parser.parseBlock();
            parser.symbolTable.out();
            return List.of(blockStmt);
        });
        parseStmtActions.put(TokenType.RETURN, parser -> List.of(parser.parseRetStmt()));
        parseStmtActions.put(TokenType.SEMICOLON, parser -> List.of(parser.parseBlankStmt()));
        parseStmtActions.put(TokenType.WHILE, parser -> List.of(parser.parseWhileStmt()));
        PARSE_STMT_ACTIONS = parseStmtActions;
        Map<TokenType, Function<SyntaxParser, ExpAST>> parseUnaryActions = new HashMap<>();
        parseUnaryActions.put(TokenType.FLOAT_LIT, parser -> new FloatLitExpAST(parser.tokens.nextFloat()));
        parseUnaryActions.put(TokenType.ID, parser -> {
            if (parser.tokens.peekType(1) == TokenType.LP) {
                return parser.parseFuncCallExp();
            } else {
                return parser.parseVarExpAST();
            }
        });
        parseUnaryActions.put(TokenType.INT_LIT, parser -> new IntLitExpAST(parser.tokens.nextInt()));
        parseUnaryActions.put(TokenType.L_NOT, parser -> {
            parser.tokens.expectAndFetch(TokenType.L_NOT);
            return new LNotExpAST(parser.parseUnary());
        });
        parseUnaryActions.put(TokenType.LP, parser -> {
            parser.tokens.expectAndFetch(TokenType.LP);
            ExpAST exp = parser.parseAddSubExp();
            parser.tokens.expectAndFetch(TokenType.RP);
            return exp;
        });
        parseUnaryActions.put(TokenType.MINUS, parser -> {
            parser.tokens.expectAndFetch(TokenType.MINUS);
            return new UnaryExpAST(UnaryExpAST.Type.NEG, parser.parseUnary());
        });
        parseUnaryActions.put(TokenType.PLUS, parser -> {
            parser.tokens.expectAndFetch(TokenType.PLUS);
            return parser.parseUnary();
        });
        PARSE_UNARY_ACTIONS = parseUnaryActions;
    }

    private boolean isProcessed;
    private final SymbolTable symbolTable;
    private final TokenList tokens;

    private RootAST rootAST;

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
        List<CompUnitAST> compUnits = new ArrayList<>();
        while (tokens.hasNext()) {
            switch (tokens.peekType()) {
                case CONST -> compUnits.addAll(parseConstDecl());
                case FLOAT, INT, VOID -> {
                    if (tokens.peekType(2) == TokenType.LP) {
                        compUnits.add(parseFuncDef());
                    } else {
                        compUnits.addAll(parseGlobalDecl());
                    }
                }
                default -> throw new RuntimeException();
            }
        }
        rootAST = new RootAST(compUnits);
    }

    private List<ConstDefAST> parseConstDecl() {
        List<ConstDefAST> constDefs = new ArrayList<>();
        tokens.expectAndFetch(TokenType.CONST);
        BasicType type = switch (tokens.expectAndFetch(TokenType.FLOAT, TokenType.INT).getType()) {
            case INT -> BasicType.I32;
            case FLOAT -> BasicType.FLOAT;
            default -> throw new RuntimeException();
        };
        do {
            constDefs.add(parseConstDef(type));
        } while (tokens.matchAndThenThrow(TokenType.COMMA));
        tokens.expectAndFetch(TokenType.SEMICOLON);
        return constDefs;
    }

    private ConstDefAST parseConstDef(BasicType type) {
        String name = tokens.nextIdentity();
        if (tokens.match(TokenType.LB)) {
            return parseConstDefForArray(type, name);
        }
        return parseConstDefForScalar(type, name);
    }

    private ConstDefAST parseConstDefForScalar(BasicType type, String name) {
        tokens.expectAndFetch(TokenType.ASSIGN);
        ExpAST rVal = parseAddSubExp();
        Number value = rVal.calc();
        if (type == BasicType.I32) {
            value = value.intValue();
        }
        return new ConstDefAST(symbolTable.makeGlobal(type, name, value));
    }

    private ConstDefAST parseConstDefForArray(BasicType type, String name) {
        List<Integer> dimensions = parseDimensionDef();
        Type targetType = type;
        for (int i = dimensions.size() - 1; i >= 0; i--) {
            targetType = new ArrayType(targetType, dimensions.get(i));
        }
        tokens.expectAndFetch(TokenType.ASSIGN);
        InitValAST initVal = parseInitVal();
        Map<Integer, ExpAST> exps = allocInitVal(initVal, dimensions);
        return new ConstDefAST(symbolTable.makeGlobal(targetType, name, exps.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, exp -> exp.getValue().calc()))));
    }

    private List<GlobalDefAST> parseGlobalDecl() {
        BasicType type = switch (tokens.expectAndFetch(TokenType.FLOAT, TokenType.INT).getType()) {
            case INT -> BasicType.I32;
            case FLOAT -> BasicType.FLOAT;
            default -> throw new RuntimeException();
        };
        List<GlobalDefAST> globalDefs = new ArrayList<>();
        do {
            GlobalDefAST globalDef = parseGlobalDef(type);
            globalDefs.add(globalDef);
        } while (tokens.matchAndThenThrow(TokenType.COMMA));
        tokens.expectAndFetch(TokenType.SEMICOLON);
        return globalDefs;
    }

    private GlobalDefAST parseGlobalDef(BasicType type) {
        String name = tokens.nextIdentity();
        if (tokens.match(TokenType.LB)) {
            return parseGlobalDefForArray(type, name);
        }
        return parseGlobalDefForScalar(type, name);
    }

    private GlobalDefAST parseGlobalDefForScalar(BasicType type, String name) {
        if (tokens.matchAndThenThrow(TokenType.ASSIGN)) {
            ExpAST rVal = parseAddSubExp();
            Number value = rVal.calc();
            if (type == BasicType.I32) {
                value = value.intValue();
            }
            return new GlobalDefAST(symbolTable.makeGlobal(type, name, value));
        }
        return new GlobalDefAST(symbolTable.makeGlobal(type, name, 0));
    }

    private GlobalDefAST parseGlobalDefForArray(BasicType type, String name) {
        List<Integer> dimensions = parseDimensionDef();
        Type targetType = type;
        for (int i = dimensions.size() - 1; i >= 0; i--) {
            targetType = new ArrayType(targetType, dimensions.get(i));
        }
        Map<Integer, Number> initValMap = new HashMap<>();
        if (tokens.matchAndThenThrow(TokenType.ASSIGN)) {
            InitValAST initVal = parseInitVal();
            Map<Integer, ExpAST> exps = allocInitVal(initVal, dimensions);
            initValMap = exps.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, exp -> exp.getValue().calc()));
        }
        return new GlobalDefAST(symbolTable.makeGlobal(targetType, name, initValMap));
    }

    private List<StmtAST> parseLocalDecl() {
        BasicType type = switch (tokens.expectAndFetch(TokenType.FLOAT, TokenType.INT).getType()) {
            case INT -> BasicType.I32;
            case FLOAT -> BasicType.FLOAT;
            default -> throw new RuntimeException();
        };
        List<StmtAST> stmts = new ArrayList<>();
        do {
            stmts.addAll(parseLocalDef(type));
        } while (tokens.matchAndThenThrow(TokenType.COMMA));
        tokens.expectAndFetch(TokenType.SEMICOLON);
        return stmts;
    }

    private List<StmtAST> parseLocalDef(BasicType type) {
        String name = tokens.nextIdentity();
        if (tokens.match(TokenType.LB)) {
            return parseLocalDefForArray(type, name);
        }
        return parseLocalDefForScalar(type, name);
    }

    private List<StmtAST> parseLocalDefForScalar(BasicType type, String name) {
        List<StmtAST> stmts = new ArrayList<>();
        LocalSymbol symbol = symbolTable.makeLocal(type, name);
        stmts.add(new LocalDefAST(symbol));
        if (tokens.matchAndThenThrow(TokenType.ASSIGN)) {
            LValAST lVal = new LValAST(symbol, List.of());
            ExpAST rVal = parseAddSubExp();
            stmts.add(new AssignStmtAST(lVal, rVal));
        }
        return stmts;
    }

    private List<StmtAST> parseLocalDefForArray(BasicType type, String name) {
        List<StmtAST> stmts = new ArrayList<>();
        List<Integer> dimensions = parseDimensionDef();
        Type targetType = type;
        for (int i = dimensions.size() - 1; i >= 0; i--) {
            targetType = new ArrayType(targetType, dimensions.get(i));
        }
        LocalSymbol symbol = symbolTable.makeLocal(targetType, name);
        stmts.add(new LocalDefAST(symbol));
        if (tokens.matchAndThenThrow(TokenType.ASSIGN)) {
            InitValAST initVal = parseInitVal();
            Map<Integer, ExpAST> exps = allocInitVal(initVal, dimensions);
            int totalSize = dimensions.stream().reduce(4, Math::multiplyExact);
            stmts.add(new ExpStmtAST(new FuncCallExpAST(symbolTable.getFunc("memset"), List.of(new VarExpAST(symbol, List.of()), new IntLitExpAST(0), new IntLitExpAST(totalSize)))));
            for (Map.Entry<Integer, ExpAST> exp : exps.entrySet()) {
                List<ExpAST> indexExps = indexExpsFromFlatten(dimensions, exp.getKey());
                stmts.add(new AssignStmtAST(new LValAST(symbol, indexExps), exp.getValue()));
            }
        }
        return stmts;
    }

    private List<ExpAST> indexExpsFromFlatten(List<Integer> dimensions, int index) {
        List<ExpAST> exps = new ArrayList<>(dimensions.size());
        ListIterator<Integer> iterator = dimensions.listIterator(dimensions.size());
        while (iterator.hasPrevious()) {
            int dimension = iterator.previous();
            exps.add(new IntLitExpAST(index % dimension));
            index /= dimension;
        }
        Collections.reverse(exps);
        return exps;
    }

    private List<Integer> parseDimensionDef() {
        List<Integer> dimensions = new ArrayList<>();
        while (tokens.matchAndThenThrow(TokenType.LB)) {
            dimensions.add(parseAddSubExp().calc().intValue());
            tokens.expectAndFetch(TokenType.RB);
        }
        return dimensions;
    }

    private CompUnitAST parseFuncDef() {
        BasicType type = switch (tokens.expectAndFetch(TokenType.FLOAT, TokenType.INT, TokenType.VOID).getType()) {
            case VOID -> BasicType.VOID;
            case INT -> BasicType.I32;
            case FLOAT -> BasicType.FLOAT;
            default -> throw new RuntimeException();
        };
        String name = tokens.nextIdentity();
        FuncSymbol decl = symbolTable.makeFunc(type, name);
        symbolTable.in();
        parseFuncDefParams(decl);
        BlockStmtAST body = parseBlock();
        symbolTable.out();
        return new FuncDefAST(decl, body);
    }

    private void parseFuncDefParams(FuncSymbol func) {
        tokens.expectAndFetch(TokenType.LP);
        while (!tokens.matchAndThenThrow(TokenType.RP)) {
            func.addParam(parseFuncDefParam());
            if (tokens.matchAndThenThrow(TokenType.RP)) {
                break;
            }
            tokens.expectAndFetch(TokenType.COMMA);
        }
    }

    private ParamSymbol parseFuncDefParam() {
        BasicType type = switch (tokens.expectAndFetch(TokenType.FLOAT, TokenType.INT).getType()) {
            case INT -> BasicType.I32;
            case FLOAT -> BasicType.FLOAT;
            default -> throw new RuntimeException();
        };
        String name = tokens.nextIdentity();
        if (!tokens.matchAndThenThrow(TokenType.LB)) {
            return symbolTable.makeParam(type, name);
        }
        tokens.expectAndFetch(TokenType.RB);
        Type targetType = type;
        while (tokens.matchAndThenThrow(TokenType.LB)) {
            ExpAST exp = parseAddSubExp();
            int dimension = exp.calc().intValue();
            targetType = new ArrayType(targetType, dimension);
            tokens.matchAndThenThrow(TokenType.RB);
        }
        targetType = new PointerType(targetType);
        return symbolTable.makeParam(targetType, name);
    }

    private BlockStmtAST parseBlock() {
        List<StmtAST> stmts = new ArrayList<>();
        tokens.expectAndFetch(TokenType.LC);
        while (!tokens.matchAndThenThrow(TokenType.RC)) {
            stmts.addAll(parseStmt());
        }
        return new BlockStmtAST(stmts);
    }

    private List<? extends StmtAST> parseStmt() {
        return PARSE_STMT_ACTIONS.get(tokens.peekType()).apply(this);
    }

    private boolean testAssignStmt() {
        int lookahead = 0;
        while (tokens.peekType(lookahead) != TokenType.SEMICOLON) {
            if (tokens.peekType(lookahead) == TokenType.ASSIGN) {
                return true;
            }
            lookahead++;
        }
        return false;
    }

    private ExpAST parseLOrExp() {
        List<ExpAST> exps = new ArrayList<>();
        exps.add(parseLAndExp());
        while (tokens.matchAndThenThrow(TokenType.L_OR)) {
            exps.add(parseLAndExp());
        }
        ExpAST root = exps.get(0);
        for (int i = 1; i < exps.size(); i++) {
            root = new LOrExpAST(root, exps.get(i));
        }
        return root;
    }

    private ExpAST parseLAndExp() {
        List<ExpAST> exps = new ArrayList<>();
        exps.add(parseEqNeExp());
        while (tokens.matchAndThenThrow(TokenType.L_AND)) {
            exps.add(parseEqNeExp());
        }
        ExpAST root = exps.get(0);
        for (int i = 1; i < exps.size(); i++) {
            root = new LAndExpAST(root, exps.get(i));
        }
        return root;
    }

    private ExpAST parseEqNeExp() {
        List<CmpExpAST.Type> types = new ArrayList<>();
        List<ExpAST> exps = new ArrayList<>();
        exps.add(parseGeGtLeLtExp());
        while (tokens.match(TokenType.EQ, TokenType.NE)) {
            types.add(switch (tokens.expectAndFetch(TokenType.EQ, TokenType.NE).getType()) {
                case EQ -> CmpExpAST.Type.EQ;
                case NE -> CmpExpAST.Type.NE;
                default -> throw new RuntimeException();
            });
            exps.add(parseGeGtLeLtExp());
        }
        ExpAST root = exps.get(0);
        for (int i = 0; i < types.size(); i++) {
            root = new CmpExpAST(types.get(i), root, exps.get(i + 1));
        }
        return root;
    }

    private ExpAST parseGeGtLeLtExp() {
        List<CmpExpAST.Type> types = new ArrayList<>();
        List<ExpAST> exps = new ArrayList<>();
        exps.add(parseAddSubExp());
        while (tokens.match(TokenType.GE, TokenType.GT, TokenType.LE, TokenType.LT)) {
            types.add(switch (tokens.expectAndFetch(TokenType.GE, TokenType.GT, TokenType.LE, TokenType.LT).getType()) {
                case GE -> CmpExpAST.Type.GE;
                case GT -> CmpExpAST.Type.GT;
                case LE -> CmpExpAST.Type.LE;
                case LT -> CmpExpAST.Type.LT;
                default -> throw new RuntimeException();
            });
            exps.add(parseAddSubExp());
        }
        ExpAST root = exps.get(0);
        for (int i = 0; i < types.size(); i++) {
            root = new CmpExpAST(types.get(i), root, exps.get(i + 1));
        }
        return root;
    }

    private ExpAST parseAddSubExp() {
        List<BinaryExpAST.Type> types = new ArrayList<>();
        List<ExpAST> exps = new ArrayList<>();
        exps.add(parseMulDivModExp());
        while (tokens.match(TokenType.PLUS, TokenType.MINUS)) {
            types.add(switch (tokens.expectAndFetch(TokenType.PLUS, TokenType.MINUS).getType()) {
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
        while (tokens.match(TokenType.MUL, TokenType.DIV, TokenType.MOD)) {
            types.add(switch (tokens.expectAndFetch(TokenType.MUL, TokenType.DIV, TokenType.MOD).getType()) {
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
        return PARSE_UNARY_ACTIONS.get(tokens.peekType()).apply(this);
    }

    private StmtAST parseAssignStmt() {
        LValAST lVal = parseLVal();
        tokens.expectAndFetch(TokenType.ASSIGN);
        ExpAST rVal = parseAddSubExp();
        tokens.expectAndFetch(TokenType.SEMICOLON);
        return new AssignStmtAST(lVal, rVal);
    }

    private BlankStmtAST parseBlankStmt() {
        tokens.expectAndFetch(TokenType.SEMICOLON);
        return new BlankStmtAST();
    }

    private BreakStmtAST parseBreakStmt() {
        tokens.expectAndFetch(TokenType.BREAK);
        tokens.expectAndFetch(TokenType.SEMICOLON);
        return new BreakStmtAST();
    }

    private ContinueStmtAST parseContinueStmt() {
        tokens.expectAndFetch(TokenType.CONTINUE);
        tokens.expectAndFetch(TokenType.SEMICOLON);
        return new ContinueStmtAST();
    }

    private ExpAST parseCond() {
        tokens.expectAndFetch(TokenType.LP);
        ExpAST cond = parseLOrExp();
        tokens.expectAndFetch(TokenType.RP);
        return cond;
    }

    private List<ExpAST> parseDimensionExp() {
        List<ExpAST> dimensions = new ArrayList<>();
        while (tokens.matchAndThenThrow(TokenType.LB)) {
            dimensions.add(parseAddSubExp());
            tokens.expectAndFetch(TokenType.RB);
        }
        return dimensions;
    }

    private ExpStmtAST parseExpStmt() {
        ExpStmtAST expStmt = new ExpStmtAST(parseAddSubExp());
        tokens.expectAndFetch(TokenType.SEMICOLON);
        return expStmt;
    }

    private FuncCallExpAST parseFuncCallExp() {
        FuncSymbol func = symbolTable.getFunc(tokens.nextIdentity());
        tokens.expectAndFetch(TokenType.LP);
        List<ExpAST> params = new ArrayList<>();
        while (!tokens.matchAndThenThrow(TokenType.RP)) {
            params.add(parseAddSubExp());
            if (tokens.matchAndThenThrow(TokenType.RP)) {
                break;
            }
            tokens.expectAndFetch(TokenType.COMMA);
        }
        return new FuncCallExpAST(func, params);
    }

    private IfStmtAST parseIfStmt() {
        tokens.expectAndFetch(TokenType.IF);
        ExpAST cond = parseCond();
        symbolTable.in();
        StmtAST stmt1 = parseStmt().get(0);
        symbolTable.out();
        if (!tokens.matchAndThenThrow(TokenType.ELSE)) {
            return new IfStmtAST(cond, stmt1, null);
        }
        symbolTable.in();
        StmtAST stmt2 = parseStmt().get(0);
        symbolTable.out();
        return new IfStmtAST(cond, stmt1, stmt2);
    }

    private InitValAST parseInitVal() {
        List<ExpAST> exps = new ArrayList<>();
        tokens.expectAndFetch(TokenType.LC);
        if (tokens.matchAndThenThrow(TokenType.RC)) {
            return new InitValAST(exps);
        }
        do {
            if (tokens.match(TokenType.LC)) {
                exps.add(parseInitVal());
            } else {
                exps.add(parseAddSubExp());
            }
        } while (tokens.matchAndThenThrow(TokenType.COMMA));
        tokens.expectAndFetch(TokenType.RC);
        return new InitValAST(exps);
    }

    private LValAST parseLVal() {
        String name = tokens.nextIdentity();
        if (tokens.match(TokenType.LB)) {
            List<ExpAST> dimensions = parseDimensionExp();
            return new LValAST(symbolTable.getData(name), dimensions);
        }
        return new LValAST(symbolTable.getData(name), List.of());
    }

    private StmtAST parseRetStmt() {
        tokens.expectAndFetch(TokenType.RETURN);
        if (tokens.matchAndThenThrow(TokenType.SEMICOLON)) {
            return new RetStmtAST(null);
        }
        ExpAST retVal = parseAddSubExp();
        tokens.expectAndFetch(TokenType.SEMICOLON);
        return new RetStmtAST(retVal);
    }

    private ExpAST parseVarExpAST() {
        String name = tokens.nextIdentity();
        if (tokens.match(TokenType.LB)) {
            List<ExpAST> dimensions = parseDimensionExp();
            return new VarExpAST(symbolTable.getData(name), dimensions);
        }
        return new VarExpAST(symbolTable.getData(name), List.of());
    }

    private WhileStmtAST parseWhileStmt() {
        tokens.expectAndFetch(TokenType.WHILE);
        ExpAST cond = parseCond();
        symbolTable.in();
        StmtAST body = parseStmt().get(0);
        symbolTable.out();
        return new WhileStmtAST(cond, body);
    }

    private static Map<Integer, ExpAST> allocInitVal(ExpAST src, List<Integer> dimensions) {
        record Task(ExpAST exp, int index, int off) {
        }
        Map<Integer, ExpAST> result = new HashMap<>();
        Queue<Task> tasks = new ArrayDeque<>();
        tasks.offer(new Task(src, 1, 0));
        while (!tasks.isEmpty()) {
            Task task = tasks.poll();
            int dim = dimensions.stream().skip(task.index()).reduce(1, Math::multiplyExact);
            int offset = task.off();
            for (ExpAST exp : (InitValAST) task.exp()) {
                if (exp instanceof InitValAST) {
                    offset = (offset + dim - 1) / dim * dim;
                    tasks.offer(new Task(exp, task.index() + 1, offset));
                    offset += dim;
                } else {
                    result.put(offset, exp);
                    offset++;
                }
            }
        }
        return result;
    }

    public RootAST getRootAST() {
        checkIfIsProcessed();
        return rootAST;
    }
}
