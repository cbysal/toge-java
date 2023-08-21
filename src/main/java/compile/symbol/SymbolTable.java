package compile.symbol;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SymbolTable extends LinkedList<Map<String, Symbol>> {
    public SymbolTable() {
        this.push(new HashMap<>());
        initBuiltInFuncs();
        initSyscalls();
    }

    private Symbol get(String name) {
        for (Map<String, Symbol> symbols : this)
            if (symbols.containsKey(name))
                return symbols.get(name);
        throw new RuntimeException("Undefined symbol: " + name);
    }

    public DataSymbol getData(String name) {
        Symbol symbol = get(name);
        if (symbol instanceof DataSymbol dataSymbol)
            return dataSymbol;
        throw new RuntimeException("Undefined data symbol: " + name);
    }

    public FuncSymbol getFunc(String name) {
        Symbol symbol = get(name);
        if (symbol instanceof FuncSymbol funcSymbol)
            return funcSymbol;
        throw new RuntimeException("Undefined function symbol: " + name);
    }

    public void in() {
        this.addFirst(new HashMap<>());
    }

    private void initBuiltInFuncs() {
        FuncSymbol func;
        func = new FuncSymbol(Type.INT, "getint");
        this.getFirst().put("getint", func);
        // getch
        func = new FuncSymbol(Type.INT, "getch");
        this.getFirst().put("getch", func);
        // getarray
        func = new FuncSymbol(Type.INT, "getarray");
        func.addParam(new ParamSymbol(Type.INT, "a", List.of(-1)));
        this.getFirst().put("getarray", func);
        // getfloat
        func = new FuncSymbol(Type.FLOAT, "getfloat");
        this.getFirst().put("getfloat", func);
        // getfarray
        func = new FuncSymbol(Type.INT, "getfarray");
        func.addParam(new ParamSymbol(Type.FLOAT, "a", List.of(-1)));
        this.getFirst().put("getfarray", func);
        // putint
        func = new FuncSymbol(Type.VOID, "putint");
        func.addParam(new ParamSymbol(Type.INT, "a"));
        this.getFirst().put("putint", func);
        // putch
        func = new FuncSymbol(Type.VOID, "putch");
        func.addParam(new ParamSymbol(Type.INT, "a"));
        this.getFirst().put("putch", func);
        // putarray
        func = new FuncSymbol(Type.VOID, "putarray");
        func.addParam(new ParamSymbol(Type.INT, "n"));
        func.addParam(new ParamSymbol(Type.INT, "a", List.of(-1)));
        this.getFirst().put("putarray", func);
        // putfloat
        func = new FuncSymbol(Type.VOID, "putfloat");
        func.addParam(new ParamSymbol(Type.FLOAT, "a"));
        this.getFirst().put("putfloat", func);
        // putfarray
        func = new FuncSymbol(Type.VOID, "putfarray");
        func.addParam(new ParamSymbol(Type.INT, "n"));
        func.addParam(new ParamSymbol(Type.FLOAT, "a", List.of(-1)));
        this.getFirst().put("putfarray", func);
        // _sysy_starttime
        func = new FuncSymbol(Type.VOID, "_sysy_starttime");
        func.addParam(new ParamSymbol(Type.INT, "lineno"));
        this.getFirst().put("_sysy_starttime", func);
        // _sysy_stoptime
        func = new FuncSymbol(Type.VOID, "_sysy_stoptime");
        func.addParam(new ParamSymbol(Type.INT, "lineno"));
        this.getFirst().put("_sysy_stoptime", func);
    }

    private void initSyscalls() {
        FuncSymbol func;
        // memset
        func = new FuncSymbol(Type.VOID, "memset");
        func.addParam(new ParamSymbol(Type.INT, "addr"));
        func.addParam(new ParamSymbol(Type.INT, "size"));
        func.addParam(new ParamSymbol(Type.INT, "value"));
        this.getFirst().put("memset", func);
    }

    public GlobalSymbol makeConst(Type type, String name, float value) {
        GlobalSymbol symbol = new GlobalSymbol(true, type, name, value);
        this.getFirst().put(name, symbol);
        return symbol;
    }

    public GlobalSymbol makeConst(Type type, String name, int value) {
        GlobalSymbol symbol = new GlobalSymbol(true, type, name, value);
        this.getFirst().put(name, symbol);
        return symbol;
    }

    public GlobalSymbol makeConst(Type type, String name, List<Integer> dimensions, Map<Integer, Integer> values) {
        GlobalSymbol symbol = new GlobalSymbol(true, type, name, dimensions, values);
        this.getFirst().put(name, symbol);
        return symbol;
    }

    public FuncSymbol makeFunc(Type type, String name) {
        FuncSymbol symbol = new FuncSymbol(type, name);
        this.getLast().put(name, symbol);
        return symbol;
    }

    public GlobalSymbol makeGlobal(Type type, String name, float value) {
        GlobalSymbol symbol = new GlobalSymbol(false, type, name, value);
        this.getFirst().put(name, symbol);
        return symbol;
    }

    public GlobalSymbol makeGlobal(Type type, String name, int value) {
        GlobalSymbol symbol = new GlobalSymbol(false, type, name, value);
        this.getFirst().put(name, symbol);
        return symbol;
    }

    public GlobalSymbol makeGlobal(Type type, String name, List<Integer> dimensions, Map<Integer, Integer> values) {
        GlobalSymbol symbol = new GlobalSymbol(false, type, name, dimensions, values);
        this.getFirst().put(name, symbol);
        return symbol;
    }

    public LocalSymbol makeLocal(Type type, String name) {
        LocalSymbol symbol = new LocalSymbol(type, name);
        this.getFirst().put(name, symbol);
        return symbol;
    }

    public LocalSymbol makeLocal(Type type, String name, List<Integer> dimensions) {
        LocalSymbol symbol = new LocalSymbol(type, name, dimensions);
        this.getFirst().put(name, symbol);
        return symbol;
    }

    public ParamSymbol makeParam(Type type, String name) {
        ParamSymbol symbol = new ParamSymbol(type, name);
        this.getFirst().put(name, symbol);
        return symbol;
    }

    public ParamSymbol makeParam(Type type, String name, List<Integer> dimensions) {
        ParamSymbol symbol = new ParamSymbol(type, name, dimensions);
        this.getFirst().put(name, symbol);
        return symbol;
    }

    public void out() {
        this.removeFirst();
    }
}
