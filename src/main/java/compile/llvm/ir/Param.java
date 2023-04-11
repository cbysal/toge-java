package compile.llvm.ir;

import compile.llvm.ir.type.Type;

public class Param extends Value {
    public Param(Type type, String name) {
        super(type, name);
    }

    @Override
    public String getTag() {
        return String.format("%%%s", name);
    }
}
