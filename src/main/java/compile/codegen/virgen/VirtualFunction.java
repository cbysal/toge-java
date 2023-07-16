package compile.codegen.virgen;

import common.Pair;
import compile.codegen.virgen.vir.VIR;
import compile.symbol.FuncSymbol;
import compile.symbol.LocalSymbol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class VirtualFunction {
    private final FuncSymbol symbol;
    private final VReg retVal;
    private final List<LocalSymbol> locals = new ArrayList<>();
    private final List<Block> blocks = new ArrayList<>();

    public VirtualFunction(FuncSymbol symbol) {
        this.symbol = symbol;
        this.retVal = switch (symbol.getType()) {
            case FLOAT, INT -> new VReg(symbol.getType());
            case VOID -> null;
        };
    }

    public void addLocal(LocalSymbol local) {
        locals.add(local);
    }

    public void addBlock(Block block) {
        blocks.add(block);
    }

    public void addBlock(int index, Block block) {
        blocks.add(index, block);
    }

    public void addBlocks(int index, Collection<Block> newBlocks) {
        blocks.addAll(index, newBlocks);
    }

    public void insertBlockAfter(Block base, Block block) {
        int index = blocks.indexOf(base);
        blocks.add(index + 1, block);
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public List<LocalSymbol> getLocals() {
        return locals;
    }

    public VReg getRetVal() {
        return retVal;
    }

    public FuncSymbol getSymbol() {
        return symbol;
    }

    public void setBlocks(List<Block> blocks) {
        this.blocks.clear();
        this.blocks.addAll(blocks);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(symbol).append('\n');
        if (!locals.isEmpty()) {
            builder.append("-------- local --------\n");
            locals.forEach(local -> builder.append(local).append('\n'));
        }
        builder.append("-------- vir --------\n");
        for (Block block : blocks) {
            builder.append(block).append(":\n");
            for (VIR ir : block)
                builder.append(ir).append('\n');
            for (Pair<Block.Cond, Block> condBlock : block.getCondBlocks()) {
                Block.Cond cond = condBlock.first();
                Block targetBlock = condBlock.second();
                builder.append("B").append(cond.type()).append("     ").append(cond.left()).append(", ").append(cond.right()).append(", ").append(targetBlock).append('\n');
            }
            if (block.getDefaultBlock() != null)
                builder.append("B       ").append(block.getDefaultBlock()).append('\n');
        }
        return builder.toString();
    }
}
