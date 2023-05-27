package compile.llvm.ir;

import compile.llvm.ir.constant.ArrConstant;
import compile.llvm.ir.constant.FloatConstant;
import compile.llvm.ir.constant.I32Constant;
import compile.llvm.ir.type.ArrayType;
import compile.llvm.ir.type.PointerType;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

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
        record Task(int offset, Value value) {
        }
        Map<Integer, Integer> result = new HashMap<>();
        Queue<Task> tasks = new ArrayDeque<>();
        tasks.offer(new Task(0, value));
        while (!tasks.isEmpty()) {
            Task task = tasks.poll();
            if (task.value() instanceof ArrConstant arrConstant) {
                int dimension = ((ArrayType) arrConstant.getType()).dimension();
                Map<Integer, Value> values = arrConstant.getValues();
                for (Map.Entry<Integer, Value> entry : values.entrySet()) {
                    tasks.offer(new Task(task.offset() * dimension + entry.getKey() * 4, entry.getValue()));
                }
            } else if (task.value() instanceof I32Constant i32Constant) {
                result.put(task.offset(), i32Constant.getValue());
            } else if (task.value() instanceof FloatConstant floatConstant) {
                result.put(task.offset(), Float.floatToIntBits(floatConstant.getValue()));
            }
        }
        return result;
    }

    @Override
    public String getTag() {
        return String.format("@%s", name);
    }

    @Override
    public String toString() {
        return String.format("@%s = dso_local global %s, align %d", name, value.getRet(), value.getType() instanceof ArrayType ? 16 : 4);
    }
}
