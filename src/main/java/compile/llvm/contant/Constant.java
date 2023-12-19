package compile.llvm.contant;

import compile.llvm.type.Type;
import compile.llvm.value.User;

public abstract class Constant extends User {
    public Constant(Type type) {
        super(type);
    }
}
