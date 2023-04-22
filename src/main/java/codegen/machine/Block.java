package codegen.machine;

import codegen.machine.asm.Asm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

public class Block implements Iterable<Asm> {
    private static int autoTag;
    private final int id;
    private final List<Asm> asms = new ArrayList<>();

    public Block() {
        this.id = autoTag++;
    }

    public void addAsm(Asm asm) {
        this.asms.add(asm);
    }

    public List<Asm> getAsms() {
        return asms;
    }

    public String getTag() {
        return String.format("b%d", id);
    }

    @Override
    public Iterator<Asm> iterator() {
        return asms.iterator();
    }

    @Override
    public void forEach(Consumer<? super Asm> action) {
        asms.forEach(action);
    }

    @Override
    public Spliterator<Asm> spliterator() {
        return asms.spliterator();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("b%d:\n", id));
        asms.forEach(asm -> builder.append("  ").append(asm).append('\n'));
        return builder.toString();
    }
}
