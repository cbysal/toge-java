package compile.codegen.virgen;

import compile.codegen.Label;
import compile.codegen.virgen.vir.VIR;

import java.util.ArrayList;
import java.util.Objects;

public class Block extends ArrayList<VIR> {
    private static int counter = 0;
    private final int id;
    private final Label label;

    public Block() {
        this.id = counter++;
        this.label = new Label();
    }

    public Label getLabel() {
        return label;
    }

    public VIR getLast() {
        if (isEmpty())
            return null;
        return get(size() - 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        Block block = (Block) o;
        return id == block.id && Objects.equals(label, block.label);
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return "b" + label.getId();
    }
}
