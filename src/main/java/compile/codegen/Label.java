package compile.codegen;

import java.util.concurrent.atomic.AtomicInteger;

public class Label {
    private static final AtomicInteger counter = new AtomicInteger(0);
    private final int id;

    public Label() {
        this.id = counter.getAndIncrement();
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "l" + id;
    }
}
