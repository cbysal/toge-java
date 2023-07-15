package compile.codegen.virgen;

import compile.codegen.Label;
import compile.codegen.virgen.vir.VIR;
import compile.codegen.virgen.vir.VIRItem;

import java.util.*;

public class Block implements Iterable<VIR> {
    public record Cond(Type type, VIRItem left, VIRItem right) {
        public enum Type {
            EQ, GE, GT, LE, LT, NE
        }
    }

    private final Label label;
    private boolean isRet;
    private Block defaultBlock;
    private final List<Map.Entry<Cond, Block>> condBlocks = new ArrayList<>();
    private final List<VIR> irs = new ArrayList<>();

    public Block() {
        this.label = new Label();
    }

    public void markRet() {
        isRet = true;
    }

    public boolean isRet() {
        return isRet;
    }

    public void setDefaultBlock(Block defaultBlock) {
        this.defaultBlock = defaultBlock;
    }

    public Block getDefaultBlock() {
        return defaultBlock;
    }

    public void setCondBlock(Map.Entry<Cond, Block> entry) {
        condBlocks.add(entry);
    }

    public void setCondBlock(Cond cond, Block condBlock) {
        condBlocks.add(Map.entry(cond, condBlock));
    }

    public List<Map.Entry<Cond, Block>> getCondBlocks() {
        return condBlocks;
    }

    public void clearCondBlocks() {
        condBlocks.clear();
    }

    public Label getLabel() {
        return label;
    }

    public boolean add(VIR ir) {
        if (isRet)
            return false;
        return irs.add(ir);
    }

    public VIR get(int index) {
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
    public Iterator<VIR> iterator() {
        return irs.iterator();
    }

    @Override
    public String toString() {
        return "b" + label.getId();
    }
}
