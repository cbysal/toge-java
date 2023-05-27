package compile.symbol;

import common.MapStack;
import compile.llvm.ir.type.BasicType;
import compile.llvm.ir.type.PointerType;
import compile.llvm.ir.type.Type;

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
        func = new FuncSymbol(BasicType.I32, "getint");
        putFirst("getint", func);
        // getch
        func = new FuncSymbol(BasicType.I32, "getch");
        putFirst("getch", func);
        // getarray
        func = new FuncSymbol(BasicType.I32, "getarray");
        func.addParam(new ParamSymbol(new PointerType(BasicType.I32), "a"));
        putFirst("getarray", func);
        // getfloat
        func = new FuncSymbol(BasicType.FLOAT, "getfloat");
        putFirst("getfloat", func);
        // getfarray
        func = new FuncSymbol(BasicType.I32, "getfarray");
        func.addParam(new ParamSymbol(new PointerType(BasicType.FLOAT), "a"));
        putFirst("getfarray", func);
        // putint
        func = new FuncSymbol(BasicType.VOID, "putint");
        func.addParam(new ParamSymbol(BasicType.I32, "a"));
        putFirst("putint", func);
        // putch
        func = new FuncSymbol(BasicType.VOID, "putch");
        func.addParam(new ParamSymbol(BasicType.I32, "a"));
        putFirst("putch", func);
        // putarray
        func = new FuncSymbol(BasicType.VOID, "putarray");
        func.addParam(new ParamSymbol(BasicType.I32, "n"));
        func.addParam(new ParamSymbol(new PointerType(BasicType.I32), "a"));
        putFirst("putarray", func);
        // putfloat
        func = new FuncSymbol(BasicType.VOID, "putfloat");
        func.addParam(new ParamSymbol(BasicType.FLOAT, "a"));
        putFirst("putfloat", func);
        // putfarray
        func = new FuncSymbol(BasicType.VOID, "putfarray");
        func.addParam(new ParamSymbol(BasicType.I32, "n"));
        func.addParam(new ParamSymbol(new PointerType(BasicType.FLOAT), "a"));
        putFirst("putfarray", func);
        // _sysy_starttime
        func = new FuncSymbol(BasicType.VOID, "_sysy_starttime");
        func.addParam(new ParamSymbol(BasicType.I32, "lineno"));
        putFirst("_sysy_starttime", func);
        // _sysy_stoptime
        func = new FuncSymbol(BasicType.VOID, "_sysy_stoptime");
        func.addParam(new ParamSymbol(BasicType.I32, "lineno"));
        putFirst("_sysy_stoptime", func);
    }

    private void initSyscalls() {
        FuncSymbol func;
        // memset
        func = new FuncSymbol(BasicType.VOID, "memset");
        func.addParam(new ParamSymbol(new PointerType(BasicType.I8), "addr"));
        func.addParam(new ParamSymbol(BasicType.I32, "size"));
        func.addParam(new ParamSymbol(BasicType.I32, "value"));
        putFirst("memset", func);
    }

    public FuncSymbol makeFunc(Type type, String name) {
        FuncSymbol symbol = new FuncSymbol(type, name);
        putFirst(name, symbol);
        return symbol;
    }

    public GlobalSymbol makeGlobal(Type type, String name, Number value) {
        GlobalSymbol symbol = new GlobalSymbol(type, name, value);
        putFirst(name, symbol);
        return symbol;
    }

    public GlobalSymbol makeGlobal(Type type, String name, Map<Integer, Number> values) {
        GlobalSymbol symbol = new GlobalSymbol(type, name, values);
        putFirst(name, symbol);
        return symbol;
    }

    public LocalSymbol makeLocal(Type type, String name) {
        LocalSymbol symbol = new LocalSymbol(type, name);
        putLast(name, symbol);
        return symbol;
    }

    public ParamSymbol makeParam(Type type, String name) {
        ParamSymbol symbol = new ParamSymbol(type, name);
        putLast(name, symbol);
        return symbol;
    }
}
