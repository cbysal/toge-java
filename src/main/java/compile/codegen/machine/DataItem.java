package compile.codegen.machine;

import java.util.HashMap;
import java.util.Map;

public class DataItem {
    private final String name;
    private final int size;
    private final Map<Integer, Integer> data = new HashMap<>();

    public DataItem(String name, int size) {
        this.name = name;
        this.size = size;
    }

    public void set(int offset, int value) {
        data.put(offset, value);
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("  .globl %s\n", name));
        builder.append("  .align 2\n");
        builder.append(String.format("  .type %s, @object\n", name));
        builder.append(String.format("  .size %s, %d\n", name, size));
        builder.append(String.format("%s:\n", name));
        if (data.isEmpty()) {
            builder.append(String.format("  .zero %d\n", size));
        } else {
            for (int pos = 0; pos < size; pos += 4) {
                builder.append(String.format("  .word %d\n", data.getOrDefault(pos, 0)));
            }
        }
        return builder.toString();
    }
}
