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

    @Override
    public Iterator<VIR> iterator() {
        return irs.iterator();
    }

    public Label getLabel() {
        return label;
    }

    boolean add(VIR ir) {
        return irs.add(ir);
    }

    VIR get(int index) {
        return irs.get(index);
    }

    boolean isEmpty() {
        return irs.isEmpty();
    }

    VIR remove(int index) {
        return irs.remove(index);
    }

    VIR set(int index, VIR ir) {
        return irs.set(index, ir);
    }

    int size() {
        return irs.size();
    }

    @Override
    public String toString() {
        return "b" + label.getId();
    }
}
