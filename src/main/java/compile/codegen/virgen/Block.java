package compile.codegen.virgen;

import compile.codegen.Label;
import compile.codegen.virgen.vir.VIR;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Block implements Iterable<VIR> {
    private final Label label;
    private final List<VIR> irs = new ArrayList<>();

    public Block() {
        this.label = new Label();
    }

    public Label getLabel() {
        return label;
    }

    public boolean add(VIR ir) {
        return irs.add(ir);
    }

    public boolean addAll(Block block) {
        return irs.addAll(block.irs);
    }

    public VIR get(int index) {
        return irs.get(index);
    }

    public VIR getLast() {
        if (irs.isEmpty())
            return null;
        return irs.get(irs.size() - 1);
    }

    public boolean isEmpty() {
        return irs.isEmpty();
    }

    public VIR remove(int index) {
        return irs.remove(index);
    }

    public VIR set(int index, VIR ir) {
        return irs.set(index, ir);
    }

    public int size() {
        return irs.size();
    }

    @Override
    public Iterator<VIR> iterator() {
        return irs.iterator();
    }

    @Override
    public String toString() {
        return "b" + label.getId();
    }
}
