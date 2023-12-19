package compile.llvm.value;

import compile.llvm.type.Type;

public abstract class User extends Value {
    public User(Type type) {
        super(type);
    }
}
