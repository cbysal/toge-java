package compile.codegen.virgen.pass;

import compile.codegen.virgen.Block;
import compile.codegen.virgen.VirtualFunction;
import compile.codegen.virgen.vir.BranchVIR;
import compile.codegen.virgen.vir.JumpVIR;
import compile.symbol.GlobalSymbol;

import java.util.*;

public class BlockFusion extends Pass {
    public BlockFusion(Set<GlobalSymbol> globals, Map<String, VirtualFunction> funcs) {
        super(globals, funcs);
    }

    @Override
    public boolean run() {
        boolean modified = false;
        for (VirtualFunction func : funcs.values()) {
            List<Block> blocks = func.getBlocks();
            Block lastBlock = blocks.get(blocks.size() - 1);
            boolean innerModified;
            do {
                innerModified = false;
                Map<Block, Set<Block>> nextBlockMap = analyzeNextBlocks(blocks);
                Map<Block, Set<Block>> prevBlockMap = analyzePrevBlocks(blocks);
                for (Block curBlock : blocks) {
                    if (nextBlockMap.get(curBlock).size() != 1)
                        continue;
                    Block nextBlock = nextBlockMap.get(curBlock).iterator().next();
                    if (curBlock == lastBlock || nextBlock == lastBlock)
                        continue;
                    if (prevBlockMap.get(nextBlock).size() != 1)
                        continue;
                    mergeBlocks(blocks, curBlock, nextBlock);
                    innerModified = true;
                    modified = true;
                    break;
                }
            } while (innerModified);
        }
        return modified;
    }

    private void mergeBlocks(List<Block> blocks, Block curBlock, Block nextBlock) {
        curBlock.remove(curBlock.size() - 1);
        curBlock.addAll(nextBlock);
        blocks.remove(nextBlock);
    }

    private Map<Block, Set<Block>> analyzeNextBlocks(List<Block> blocks) {
        Map<Block, Set<Block>> nextBlockMap = new HashMap<>();
        for (Block block : blocks)
            nextBlockMap.put(block, new HashSet<>());
        for (Block block : blocks) {
            if (block.getLast() instanceof BranchVIR branchVIR) {
                nextBlockMap.get(block).add(branchVIR.trueBlock());
                nextBlockMap.get(block).add(branchVIR.falseBlock());
            } else if (block.getLast() instanceof JumpVIR jumpVIR)
                nextBlockMap.get(block).add(jumpVIR.target());
        }
        return nextBlockMap;
    }

    private Map<Block, Set<Block>> analyzePrevBlocks(List<Block> blocks) {
        Map<Block, Set<Block>> prevBlockMap = new HashMap<>();
        for (Block block : blocks)
            prevBlockMap.put(block, new HashSet<>());
        for (Block block : blocks) {
            if (block.getLast() instanceof BranchVIR branchVIR) {
                prevBlockMap.get(branchVIR.trueBlock()).add(block);
                prevBlockMap.get(branchVIR.falseBlock()).add(block);
            } else if (block.getLast() instanceof JumpVIR jumpVIR)
                prevBlockMap.get(jumpVIR.target()).add(block);
        }
        return prevBlockMap;
    }
}
