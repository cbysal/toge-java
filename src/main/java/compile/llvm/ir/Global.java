package compile.llvm.ir;

import compile.llvm.ir.type.ArrayType;
import compile.llvm.ir.type.PointerType;

public class Global extends Value {
    private final Value value;
    // TODO const flag

    public Global(String name, Value value) {
        super(new PointerType(value.getType()), name);
        this.value = value;
    }

    @Override
    public String getTag() {
        return String.format("@%s", name);
    }

    @Override
    public String toString() {
        return String.format("@%s = dso_local global %s, align %d", name, value.getRet(),
                value.getType() instanceof ArrayType ? 16 : 4);
    }
}
