package compile.llvm.contant;

import compile.llvm.type.Type;

public class ConstantZero extends Constant {
    public ConstantZero(Type type) {
        super(type);
    }

    @Override
    public String getName() {
        return "zeroinitializer";
    }

    @Override
    public String toString() {
        return String.format("%s zeroinitializer", type);
    }
}
