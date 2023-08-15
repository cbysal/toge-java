package compile.codegen.virgen;

import common.Pair;
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
    private Block defaultBlock;
    private final List<Pair<Cond, Block>> condBlocks = new ArrayList<>();
    private final Map<VReg, Set<VReg>> phiMap = new HashMap<>();
    private final List<VIR> irs = new ArrayList<>();

    public Block() {
        this.label = new Label();
    }

    public void setDefaultBlock(Block defaultBlock) {
        this.defaultBlock = defaultBlock;
    }

    public Block getDefaultBlock() {
        return defaultBlock;
    }

    public void setCondBlock(Pair<Cond, Block> entry) {
        condBlocks.add(entry);
    }

    public void setCondBlock(Cond cond, Block condBlock) {
        condBlocks.add(new Pair<>(cond, condBlock));
    }

    public List<Pair<Cond, Block>> getCondBlocks() {
        return condBlocks;
    }

    public void clearCondBlocks() {
        condBlocks.clear();
    }

    public Map<VReg, Set<VReg>> getPhiMap() {
        return phiMap;
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

    public boolean addAll(int index, List<VIR> irs) {
        return this.irs.addAll(index, irs);
    }

    public VIR get(int index) {
        return irs.get(index);
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
