package compile.llvm.ir.constant;

import compile.llvm.ir.Value;
import compile.llvm.ir.type.ArrayType;
import compile.llvm.ir.type.Type;

import java.util.Map;

public class ArrConstant extends Constant {
    private final Map<Integer, Value> values;

    public ArrConstant(Type type, int dimension, Map<Integer, Value> values) {
        super(new ArrayType(type, dimension));
        this.values = values;
    }

    public Map<Integer, Value> getValues() {
        return values;
    }

    @Override
    public String getTag() {
        StringBuilder builder = new StringBuilder();
        Type type = this.type;
        builder.append('[');
        boolean isFirst = true;
        for (int i = 0; i < ((ArrayType) type).dimension(); i++) {
            if (!isFirst) {
                builder.append(", ");
            }
            if (values.containsKey(i)) {
                builder.append(values.get(i).getRet());
            } else {
                builder.append("zeroinitializer");
            }
            isFirst = false;
        }
        builder.append(']');
        return builder.toString();
    }

    @Override
    public String toString() {
        return String.format("@%s = dso_local global %s, align 16", name, getRet());
    }
}
