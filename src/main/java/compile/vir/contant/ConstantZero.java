package compile.vir.contant;

import compile.vir.type.Type;

public class ConstantZero extends Constant {
    public ConstantZero(Type type) {
        super(type);
    }

    @Override
    public String getName() {
        return "zeroinitializer";
    }
}
