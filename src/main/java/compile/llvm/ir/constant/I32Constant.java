package compile.llvm.ir.constant;

import compile.llvm.ir.type.BasicType;

public class I32Constant extends Constant {
    private final int value;

    public I32Constant(int value) {
        super(BasicType.I32);
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String getTag() {
        return Integer.toString(value);
    }
}
