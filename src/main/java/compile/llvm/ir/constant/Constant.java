package compile.llvm.ir.constant;

import compile.llvm.ir.Value;
import compile.llvm.ir.type.Type;

public abstract class Constant extends Value {
    Constant(Type type) {
        super(type);
    }
}
