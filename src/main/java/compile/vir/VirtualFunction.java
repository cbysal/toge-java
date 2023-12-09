package compile.vir;

import compile.vir.ir.VIR;
import compile.symbol.FuncSymbol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class VirtualFunction {
    private final FuncSymbol symbol;
    private final List<Block> blocks = new ArrayList<>();

    public VirtualFunction(FuncSymbol symbol) {
        this.symbol = symbol;
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

    public void setBlocks(List<Block> blocks) {
        this.blocks.clear();
        this.blocks.addAll(blocks);
    }

    public FuncSymbol getSymbol() {
        return symbol;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(symbol).append('\n');
        for (Block block : blocks) {
            builder.append(block).append(":\n");
            for (VIR ir : block)
                builder.append(ir).append('\n');
        }
        return builder.toString();
    }
}
