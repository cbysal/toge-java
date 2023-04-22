package compile.llvm.ir.constant;

import compile.llvm.ir.type.BasicType;

public class FloatConstant extends Constant {
    private final float value;

    public FloatConstant(float value) {
        super(BasicType.FLOAT);
        this.value = value;
    }

    public float getValue() {
        return value;
    }

    @Override
    public String getTag() {
        return String.format("0x%x", Double.doubleToLongBits(value));
    }
}
