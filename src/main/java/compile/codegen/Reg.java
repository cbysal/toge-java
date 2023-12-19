package compile.codegen;

import compile.llvm.type.Type;
import compile.llvm.value.Value;

public abstract class Reg extends Value {
    protected Reg(Type type) {
        super(type);
    }
}
