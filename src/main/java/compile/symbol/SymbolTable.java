package compile.symbol;

import java.util.*;

public class SymbolTable {
    private final List<Map<String, Symbol>> tableStack = new ArrayList<>();

    public SymbolTable() {
        tableStack.add(new HashMap<>());
        initBuiltInFuncs();
        initSyscalls();
    }

    private Symbol get(String name) {
        ListIterator<Map<String, Symbol>> iterator = tableStack.listIterator(tableStack.size());
        while (iterator.hasPrevious()) {
            Map<String, Symbol> table = iterator.previous();
            if (table.containsKey(name)) {
                return table.get(name);
            }
        }
        throw new RuntimeException("Undefined symbol: " + name);
    }

    public DataSymbol getData(String name) {
        Symbol symbol = get(name);
        if (symbol instanceof DataSymbol dataSymbol) {
            return dataSymbol;
        }
        throw new RuntimeException("Undefined data symbol: " + name);
    }

    public FuncSymbol getFunc(String name) {
        Symbol symbol = get(name);
        if (symbol instanceof FuncSymbol funcSymbol) {
            return funcSymbol;
        }
        throw new RuntimeException("Undefined function symbol: " + name);
    }

    public void in() {
        tableStack.add(new HashMap<>());
    }

    private void initBuiltInFuncs() {
        Map<String, Symbol> rootTable = tableStack.get(0);
        FuncSymbol func;
        func = new FuncSymbol(false, "getint");
        rootTable.put("getint", func);
        // getch
        func = new FuncSymbol(false, "getch");
        rootTable.put("getch", func);
        // getarray
        func = new FuncSymbol(false, "getarray");
        func.addParam(new ParamSymbol(false, "a", List.of(-1)));
        rootTable.put("getarray", func);
        // getfloat
        func = new FuncSymbol(true, "getfloat");
        rootTable.put("getfloat", func);
        // getfarray
        func = new FuncSymbol(false, "getfarray");
        func.addParam(new ParamSymbol(true, "a", List.of(-1)));
        rootTable.put("getfarray", func);
        // putint
        func = new FuncSymbol("putint");
        func.addParam(new ParamSymbol(false, "a"));
        rootTable.put("putint", func);
        // putch
        func = new FuncSymbol("putch");
        func.addParam(new ParamSymbol(false, "a"));
        rootTable.put("putch", func);
        // putarray
        func = new FuncSymbol("putarray");
        func.addParam(new ParamSymbol(false, "n"));
        func.addParam(new ParamSymbol(false, "a", List.of(-1)));
        rootTable.put("putarray", func);
        // putfloat
        func = new FuncSymbol("putfloat");
        func.addParam(new ParamSymbol(true, "a"));
        rootTable.put("putfloat", func);
        // putfarray
        func = new FuncSymbol("putfarray");
        func.addParam(new ParamSymbol(false, "n"));
        func.addParam(new ParamSymbol(true, "a", List.of(-1)));
        rootTable.put("putfarray", func);
        // _sysy_starttime
        func = new FuncSymbol("_sysy_starttime");
        func.addParam(new ParamSymbol(false, "lineno"));
        rootTable.put("_sysy_starttime", func);
        // _sysy_stoptime
        func = new FuncSymbol("_sysy_stoptime");
        func.addParam(new ParamSymbol(false, "lineno"));
        rootTable.put("_sysy_stoptime", func);
    }

    private void initSyscalls() {
        Map<String, Symbol> rootTable = tableStack.get(0);
        FuncSymbol func;
        // memset
        func = new FuncSymbol("memset");
        func.addParam(new ParamSymbol(false, "addr"));
        func.addParam(new ParamSymbol(false, "size"));
        func.addParam(new ParamSymbol(false, "value"));
        rootTable.put("memset", func);
    }

    public ConstSymbol makeConst(boolean isFloat, String name, float value) {
        Map<String, Symbol> table = tableStack.get(tableStack.size() - 1);
        ConstSymbol symbol = new ConstSymbol(isFloat, name, value);
        table.put(name, symbol);
        return symbol;
    }

    public ConstSymbol makeConst(boolean isFloat, String name, int value) {
        Map<String, Symbol> table = tableStack.get(tableStack.size() - 1);
        ConstSymbol symbol = new ConstSymbol(isFloat, name, value);
        table.put(name, symbol);
        return symbol;
    }

    public ConstSymbol makeConst(boolean isFloat, String name, List<Integer> dimensions, Map<Integer, Integer> values) {
        Map<String, Symbol> table = tableStack.get(tableStack.size() - 1);
        ConstSymbol symbol = new ConstSymbol(isFloat, name, dimensions, values);
        table.put(name, symbol);
        return symbol;
    }

    public FuncSymbol makeFunc(String name) {
        Map<String, Symbol> table = tableStack.get(0);
        FuncSymbol symbol = new FuncSymbol(name);
        table.put(name, symbol);
        return symbol;
    }

    public FuncSymbol makeFunc(boolean isFloat, String name) {
        Map<String, Symbol> table = tableStack.get(0);
        FuncSymbol symbol = new FuncSymbol(isFloat, name);
        table.put(name, symbol);
        return symbol;
    }

    public GlobalSymbol makeGlobal(boolean isFloat, String name, float value) {
        Map<String, Symbol> table = tableStack.get(0);
        GlobalSymbol symbol = new GlobalSymbol(isFloat, name, value);
        table.put(name, symbol);
        return symbol;
    }

    public GlobalSymbol makeGlobal(boolean isFloat, String name, int value) {
        Map<String, Symbol> table = tableStack.get(0);
        GlobalSymbol symbol = new GlobalSymbol(isFloat, name, value);
        table.put(name, symbol);
        return symbol;
    }

    public GlobalSymbol makeGlobal(boolean isFloat, String name, List<Integer> dimensions,
                                   Map<Integer, Integer> values) {
        Map<String, Symbol> table = tableStack.get(0);
        GlobalSymbol symbol = new GlobalSymbol(isFloat, name, dimensions, values);
        table.put(name, symbol);
        return symbol;
    }

    public LocalSymbol makeLocal(boolean isFloat, String name) {
        Map<String, Symbol> table = tableStack.get(tableStack.size() - 1);
        LocalSymbol symbol = new LocalSymbol(isFloat, name);
        table.put(name, symbol);
        return symbol;
    }

    public LocalSymbol makeLocal(boolean isFloat, String name, List<Integer> dimensions) {
        Map<String, Symbol> table = tableStack.get(tableStack.size() - 1);
        LocalSymbol symbol = new LocalSymbol(isFloat, name, dimensions);
        table.put(name, symbol);
        return symbol;
    }

    public ParamSymbol makeParam(boolean isFloat, String name) {
        Map<String, Symbol> table = tableStack.get(tableStack.size() - 1);
        ParamSymbol symbol = new ParamSymbol(isFloat, name);
        table.put(name, symbol);
        return symbol;
    }

    public ParamSymbol makeParam(boolean isFloat, String name, List<Integer> dimensions) {
        Map<String, Symbol> table = tableStack.get(tableStack.size() - 1);
        ParamSymbol symbol = new ParamSymbol(isFloat, name, dimensions);
        table.put(name, symbol);
        return symbol;
    }

    public void out() {
        tableStack.remove(tableStack.size() - 1);
    }
}
