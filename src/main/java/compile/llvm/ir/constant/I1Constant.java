package compile.llvm.ir.constant;

import compile.llvm.ir.type.BasicType;

public class I1Constant extends Constant {
    private final boolean value;

    private I1Constant(boolean value) {
        super(BasicType.I1);
        this.value = value;
    }

    public static final I1Constant TRUE = new I1Constant(true);
    public static final I1Constant FALSE = new I1Constant(false);

    @Override
    public String getTag() {
        return Boolean.toString(value);
    }
}
