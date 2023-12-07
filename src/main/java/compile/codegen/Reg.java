package compile.codegen;

import compile.vir.type.Type;
import compile.vir.value.Value;

public abstract class Reg extends Value {
    protected Reg(Type type) {
        super(type);
    }
}
