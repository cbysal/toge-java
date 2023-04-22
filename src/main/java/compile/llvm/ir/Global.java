package compile.llvm.ir;

import compile.llvm.ir.constant.ArrConstant;
import compile.llvm.ir.constant.FloatConstant;
import compile.llvm.ir.constant.I32Constant;
import compile.llvm.ir.constant.ZeroConstant;
import compile.llvm.ir.type.ArrayType;
import compile.llvm.ir.type.PointerType;

import java.util.HashMap;
import java.util.Map;

public class Global extends Value {
    private final Value value;
    // TODO const flag

    public Global(String name, Value value) {
        super(new PointerType(value.getType()), name);
        this.value = value;
    }

    public Value getValue() {
        return value;
    }

    public Map<Integer, Integer> flatten() {
        Map<Integer, Integer> result = new HashMap<>();
        flattenDfs(result, 0, value);
        return result;
    }

    private void flattenDfs(Map<Integer, Integer> result, int pos, Value value) {
        if (value instanceof ZeroConstant) {
            return;
        }
        if (value instanceof I32Constant i32Constant) {
            result.put(pos, i32Constant.getValue());
            return;
        }
        if (value instanceof FloatConstant floatConstant) {
            result.put(pos, Float.floatToIntBits(floatConstant.getValue()));
            return;
        }
        if (value instanceof ArrConstant arrConstant) {
            int dimension = ((ArrayType) arrConstant.getType()).dimension();
            Map<Integer, Value> values = arrConstant.getValues();
            for (Map.Entry<Integer, Value> valueEntry : values.entrySet()) {
                flattenDfs(result, pos * dimension + valueEntry.getKey() * 4, valueEntry.getValue());
            }
            return;
        }
        throw new RuntimeException("Unhandled value: " + value);
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
