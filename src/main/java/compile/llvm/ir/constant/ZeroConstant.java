package compile.llvm.ir.constant;

import compile.llvm.ir.type.Type;

public class ZeroConstant extends Constant {
    public ZeroConstant(Type type) {
        super(type);
    }

    @Override
    public String getTag() {
        return "zeroinitializer";
    }
}
