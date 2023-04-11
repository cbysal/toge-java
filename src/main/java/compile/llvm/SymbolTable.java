package compile.llvm;

import common.MapStack;
import compile.llvm.ir.Value;
import compile.llvm.ir.instr.Instr;

class SymbolTable extends MapStack<String, Value> {
    public Value getValue(String name) {
        Value value = get(name);
        if (value == null) {
            throw new RuntimeException("No such instruction: " + name);
        }
        return value;
    }

    public Instr getInstr(String name) {
        Value value = get(name);
        if (value instanceof Instr instr) {
            return instr;
        }
        throw new RuntimeException("No such instruction: " + name);
    }

    @Override
    public void putFirst(String name, Value value) {
        super.putFirst(name, value);
    }

    @Override
    public void putLast(String name, Value value) {
        super.putLast(name, value);
    }
}
