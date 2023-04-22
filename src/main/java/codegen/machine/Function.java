package codegen.machine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

public class Function implements Iterable<Block> {
    private final String name;
    private final List<Block> blocks = new ArrayList<>();

    public Function(String name) {
        this.name = name;
    }

    public void addBlock(Block block) {
        blocks.add(block);
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    @Override
    public Iterator<Block> iterator() {
        return blocks.iterator();
    }

    @Override
    public void forEach(Consumer<? super Block> action) {
        blocks.forEach(action);
    }

    @Override
    public Spliterator<Block> spliterator() {
        return blocks.spliterator();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("  .globl ").append(name).append('\n');
        builder.append("  .p2align 1\n");
        builder.append("  .type ").append(name).append("@function\n");
        builder.append(name).append(":\n");
        blocks.forEach(builder::append);
        return builder.toString();
    }
}
