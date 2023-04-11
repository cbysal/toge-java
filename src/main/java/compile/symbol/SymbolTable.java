package compile.symbol;

import common.MapStack;

import java.util.List;
import java.util.Map;

public class SymbolTable extends MapStack<String, Symbol> {
    public SymbolTable() {
        initBuiltInFuncs();
        initSyscalls();
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

    private void initBuiltInFuncs() {
        FuncSymbol func;
        func = new FuncSymbol(false, "getint");
        putFirst("getint", func);
        // getch
        func = new FuncSymbol(false, "getch");
        putFirst("getch", func);
        // getarray
        func = new FuncSymbol(false, "getarray");
        func.addParam(new ParamSymbol(false, "a", List.of(-1)));
        putFirst("getarray", func);
        // getfloat
        func = new FuncSymbol(true, "getfloat");
        putFirst("getfloat", func);
        // getfarray
        func = new FuncSymbol(false, "getfarray");
        func.addParam(new ParamSymbol(true, "a", List.of(-1)));
        putFirst("getfarray", func);
        // putint
        func = new FuncSymbol("putint");
        func.addParam(new ParamSymbol(false, "a"));
        putFirst("putint", func);
        // putch
        func = new FuncSymbol("putch");
        func.addParam(new ParamSymbol(false, "a"));
        putFirst("putch", func);
        // putarray
        func = new FuncSymbol("putarray");
        func.addParam(new ParamSymbol(false, "n"));
        func.addParam(new ParamSymbol(false, "a", List.of(-1)));
        putFirst("putarray", func);
        // putfloat
        func = new FuncSymbol("putfloat");
        func.addParam(new ParamSymbol(true, "a"));
        putFirst("putfloat", func);
        // putfarray
        func = new FuncSymbol("putfarray");
        func.addParam(new ParamSymbol(false, "n"));
        func.addParam(new ParamSymbol(true, "a", List.of(-1)));
        putFirst("putfarray", func);
        // _sysy_starttime
        func = new FuncSymbol("_sysy_starttime");
        func.addParam(new ParamSymbol(false, "lineno"));
        putFirst("_sysy_starttime", func);
        // _sysy_stoptime
        func = new FuncSymbol("_sysy_stoptime");
        func.addParam(new ParamSymbol(false, "lineno"));
        putFirst("_sysy_stoptime", func);
    }

    private void initSyscalls() {
        FuncSymbol func;
        // memset
        func = new FuncSymbol("memset");
        func.addParam(new ParamSymbol(false, "addr"));
        func.addParam(new ParamSymbol(false, "size"));
        func.addParam(new ParamSymbol(false, "value"));
        putFirst("memset", func);
    }

    public ConstSymbol makeConst(boolean isFloat, String name, float value) {
        ConstSymbol symbol = new ConstSymbol(isFloat, name, value);
        putLast(name, symbol);
        return symbol;
    }

    public ConstSymbol makeConst(boolean isFloat, String name, int value) {
        ConstSymbol symbol = new ConstSymbol(isFloat, name, value);
        putLast(name, symbol);
        return symbol;
    }

    public ConstSymbol makeConst(boolean isFloat, String name, List<Integer> dimensions, Map<Integer, Integer> values) {
        ConstSymbol symbol = new ConstSymbol(isFloat, name, dimensions, values);
        putLast(name, symbol);
        return symbol;
    }

    public FuncSymbol makeFunc(String name) {
        FuncSymbol symbol = new FuncSymbol(name);
        putFirst(name, symbol);
        return symbol;
    }

    public FuncSymbol makeFunc(boolean isFloat, String name) {
        FuncSymbol symbol = new FuncSymbol(isFloat, name);
        putFirst(name, symbol);
        return symbol;
    }

    public GlobalSymbol makeGlobal(boolean isFloat, String name, float value) {
        GlobalSymbol symbol = new GlobalSymbol(isFloat, name, value);
        putFirst(name, symbol);
        return symbol;
    }

    public GlobalSymbol makeGlobal(boolean isFloat, String name, int value) {
        GlobalSymbol symbol = new GlobalSymbol(isFloat, name, value);
        putFirst(name, symbol);
        return symbol;
    }

    public GlobalSymbol makeGlobal(boolean isFloat, String name, List<Integer> dimensions,
                                   Map<Integer, Integer> values) {
        GlobalSymbol symbol = new GlobalSymbol(isFloat, name, dimensions, values);
        putFirst(name, symbol);
        return symbol;
    }

    public LocalSymbol makeLocal(boolean isFloat, String name) {
        LocalSymbol symbol = new LocalSymbol(isFloat, name);
        putLast(name, symbol);
        return symbol;
    }

    public LocalSymbol makeLocal(boolean isFloat, String name, List<Integer> dimensions) {
        LocalSymbol symbol = new LocalSymbol(isFloat, name, dimensions);
        putLast(name, symbol);
        return symbol;
    }

    public ParamSymbol makeParam(boolean isFloat, String name) {
        ParamSymbol symbol = new ParamSymbol(isFloat, name);
        putLast(name, symbol);
        return symbol;
    }

    public ParamSymbol makeParam(boolean isFloat, String name, List<Integer> dimensions) {
        ParamSymbol symbol = new ParamSymbol(isFloat, name, dimensions);
        putLast(name, symbol);
        return symbol;
    }
}
