package compile.symbol;

import compile.vir.Argument;
import compile.vir.GlobalVariable;
import compile.vir.contant.Constant;
import compile.vir.contant.ConstantArray;
import compile.vir.contant.ConstantNumber;
import compile.vir.ir.AllocaVIR;
import compile.vir.type.ArrayType;
import compile.vir.type.BasicType;
import compile.vir.type.PointerType;
import compile.vir.type.Type;
import compile.vir.value.Value;

import java.util.*;

public class SymbolTable extends LinkedList<Map<String, Value>> {
    public SymbolTable() {
        this.push(new HashMap<>());
        initBuiltInFuncs();
        initSyscalls();
    }

    private Value get(String name) {
        for (Map<String, Value> symbols : this)
            if (symbols.containsKey(name))
                return symbols.get(name);
        throw new RuntimeException("Undefined symbol: " + name);
    }

    public Value getData(String name) {
        return get(name);
    }

    public FuncSymbol getFunc(String name) {
        Value symbol = get(name);
        if (symbol instanceof FuncSymbol funcSymbol)
            return funcSymbol;
        throw new RuntimeException("Undefined function symbol: " + name);
    }

    public void in() {
        this.addFirst(new HashMap<>());
    }

    private void initBuiltInFuncs() {
        FuncSymbol func;
        func = new FuncSymbol(BasicType.I32, "getint");
        this.getFirst().put("getint", func);
        // getch
        func = new FuncSymbol(BasicType.I32, "getch");
        this.getFirst().put("getch", func);
        // getarray
        func = new FuncSymbol(BasicType.I32, "getarray");
        func.addArg(new Argument(new PointerType(BasicType.I32), "a"));
        this.getFirst().put("getarray", func);
        // getfloat
        func = new FuncSymbol(BasicType.FLOAT, "getfloat");
        this.getFirst().put("getfloat", func);
        // getfarray
        func = new FuncSymbol(BasicType.I32, "getfarray");
        func.addArg(new Argument(new PointerType(BasicType.FLOAT), "a"));
        this.getFirst().put("getfarray", func);
        // putint
        func = new FuncSymbol(BasicType.VOID, "putint");
        func.addArg(new Argument(BasicType.I32, "a"));
        this.getFirst().put("putint", func);
        // putch
        func = new FuncSymbol(BasicType.VOID, "putch");
        func.addArg(new Argument(BasicType.I32, "a"));
        this.getFirst().put("putch", func);
        // putarray
        func = new FuncSymbol(BasicType.VOID, "putarray");
        func.addArg(new Argument(BasicType.I32, "n"));
        func.addArg(new Argument(new PointerType(BasicType.I32), "a"));
        this.getFirst().put("putarray", func);
        // putfloat
        func = new FuncSymbol(BasicType.VOID, "putfloat");
        func.addArg(new Argument(BasicType.FLOAT, "a"));
        this.getFirst().put("putfloat", func);
        // putfarray
        func = new FuncSymbol(BasicType.VOID, "putfarray");
        func.addArg(new Argument(BasicType.I32, "n"));
        func.addArg(new Argument(new PointerType(BasicType.FLOAT), "a"));
        this.getFirst().put("putfarray", func);
        // _sysy_starttime
        func = new FuncSymbol(BasicType.VOID, "_sysy_starttime");
        func.addArg(new Argument(BasicType.I32, "lineno"));
        this.getFirst().put("_sysy_starttime", func);
        // _sysy_stoptime
        func = new FuncSymbol(BasicType.VOID, "_sysy_stoptime");
        func.addArg(new Argument(BasicType.I32, "lineno"));
        this.getFirst().put("_sysy_stoptime", func);
    }

    private void initSyscalls() {
        FuncSymbol func;
        // memset
        func = new FuncSymbol(BasicType.VOID, "memset");
        func.addArg(new Argument(BasicType.I32, "addr"));
        func.addArg(new Argument(BasicType.I32, "size"));
        func.addArg(new Argument(BasicType.I32, "value"));
        this.getFirst().put("memset", func);
    }

    public GlobalVariable makeConst(Type type, String name, float value) {
        GlobalVariable symbol = new GlobalVariable(true, type, name, new ConstantNumber(value));
        this.getFirst().put(name, symbol);
        return symbol;
    }

    public GlobalVariable makeConst(Type type, String name, int value) {
        GlobalVariable symbol = new GlobalVariable(true, type, name, new ConstantNumber(value));
        this.getFirst().put(name, symbol);
        return symbol;
    }

    private Constant fuseConst(Type type, Map<Integer, Integer> values) {
        List<ArrayType> arrayTypes = new ArrayList<>();
        while (type instanceof ArrayType arrayType) {
            arrayTypes.add(arrayType);
            type = arrayType.getBaseType();
        }
        Collections.reverse(arrayTypes);
        int totalSize = arrayTypes.stream().mapToInt(ArrayType::getArraySize).reduce(1, Math::multiplyExact);
        List<Constant> constants = new ArrayList<>();
        for (int i = 0; i < totalSize; i++) {
            ConstantNumber number = switch (type) {
                case BasicType.I32 -> new ConstantNumber(values.getOrDefault(i, 0));
                case BasicType.FLOAT -> new ConstantNumber(Float.intBitsToFloat(values.getOrDefault(i, 0)));
                default -> throw new IllegalStateException("Unexpected value: " + type);
            };
            constants.add(number);
        }
        for (ArrayType arrayType : arrayTypes) {
            List<Constant> newConstants = new ArrayList<>();
            for (int i = 0; i < constants.size(); i += arrayType.getArraySize()) {
                List<Constant> subConstants = constants.subList(i, i + arrayType.getArraySize());
                ConstantArray constantArray = new ConstantArray(arrayType, subConstants);
                newConstants.add(constantArray);
            }
            constants = newConstants;
        }
        return constants.getFirst();
    }

    public GlobalVariable makeConst(Type type, String name, List<Integer> dimensions, Map<Integer, Integer> values) {

        GlobalVariable symbol = new GlobalVariable(true, type, name, fuseConst(type, values));
        this.getFirst().put(name, symbol);
        return symbol;
    }

    public FuncSymbol makeFunc(Type type, String name) {
        FuncSymbol symbol = new FuncSymbol(type, name);
        this.getLast().put(name, symbol);
        return symbol;
    }

    public GlobalVariable makeGlobal(Type type, String name, float value) {
        GlobalVariable symbol = new GlobalVariable(false, type, name, new ConstantNumber(value));
        this.getFirst().put(name, symbol);
        return symbol;
    }

    public GlobalVariable makeGlobal(Type type, String name, int value) {
        GlobalVariable symbol = new GlobalVariable(false, type, name, new ConstantNumber(value));
        this.getFirst().put(name, symbol);
        return symbol;
    }

    public GlobalVariable makeGlobal(Type type, String name, List<Integer> dimensions, Map<Integer, Integer> values) {
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
