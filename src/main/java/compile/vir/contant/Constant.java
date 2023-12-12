package compile.vir.contant;

import compile.vir.type.Type;
import compile.vir.value.User;

public abstract class Constant extends User {
    public Constant(Type type) {
        super(type);
    }
}
