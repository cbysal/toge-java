package compile.codegen.virgen.pass;

import common.Pair;
import compile.codegen.virgen.Block;
import compile.codegen.virgen.VReg;
import compile.codegen.virgen.VirtualFunction;
import compile.codegen.virgen.vir.VIR;
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
                    if (checkFusionability(curBlock, nextBlock)) {
                        mergeBlocks(blocks, curBlock, nextBlock);
                        innerModified = true;
                        modified = true;
                        break;
                    }
                }
            } while (innerModified);
        }
        return modified;
    }

    private boolean checkFusionability(Block block1, Block block2) {
        Set<VReg> defRegs = new HashSet<>(block1.getPhiMap().keySet());
        for (VIR ir : block1)
            if (ir.getWrite() != null)
                defRegs.add(ir.getWrite());
        for (Set<VReg> sources : block2.getPhiMap().values())
            for (VReg reg : sources)
                if (defRegs.contains(reg))
                    return false;
        return true;
    }

    private void mergeBlocks(List<Block> blocks, Block curBlock, Block nextBlock) {
        Map<VReg, Set<VReg>> curPhiMap = curBlock.getPhiMap();
        Map<VReg, Set<VReg>> nextPhiMap = nextBlock.getPhiMap();
        for (Map.Entry<VReg, Set<VReg>> entry : nextPhiMap.entrySet()) {
            VReg target = entry.getKey();
            Set<VReg> sources = entry.getValue();
            if (curPhiMap.containsKey(target))
                curPhiMap.get(target).addAll(sources);
            else
                curPhiMap.put(target, sources);
        }
        curBlock.addAll(nextBlock);
        curBlock.clearCondBlocks();
        nextBlock.getCondBlocks().forEach(curBlock::setCondBlock);
        curBlock.setDefaultBlock(nextBlock.getDefaultBlock());
        blocks.remove(nextBlock);
        for (VirtualFunction func : funcs.values())
            removePhiConflict(func);
    }

    private void removePhiConflict(VirtualFunction func) {
        Map<VReg, Block> regToBlockMap = new HashMap<>();
        for (Block block : func.getBlocks()) {
            for (VReg target : block.getPhiMap().keySet())
                regToBlockMap.put(target, block);
            for (VIR ir : block)
                if (ir.getWrite() != null)
                    regToBlockMap.put(ir.getWrite(), block);
        }
        boolean modified;
        do {
            modified = false;
            for (Block curBlock : func.getBlocks()) {
                for (Set<VReg> sources : curBlock.getPhiMap().values()) {
                    Map<Block, Integer> counter = new HashMap<>();
                    for (VReg source : sources) {
                        Block block = regToBlockMap.get(source);
                        counter.put(block, counter.getOrDefault(block, 0) + 1);
                    }
                    List<Block> toProcessBlocks =
                            counter.keySet().stream().filter(block -> counter.get(block) > 1).toList();
                    modified |= !toProcessBlocks.isEmpty();
                    for (Block toProcessBlock : toProcessBlocks) {
                        Set<VReg> conflictRegs = new HashSet<>();
                        for (VReg source : sources)
                            if (regToBlockMap.get(source) == toProcessBlock)
                                conflictRegs.add(source);
                        Set<VReg> toFillInSources = new HashSet<>();
                        for (Map.Entry<VReg, Set<VReg>> entry : toProcessBlock.getPhiMap().entrySet())
                            if (conflictRegs.contains(entry.getKey()))
                                toFillInSources.addAll(entry.getValue());
                        for (VIR ir : toProcessBlock) {
                            if (ir.getWrite() != null && conflictRegs.contains(ir.getWrite())) {
                                toFillInSources.clear();
                                toFillInSources.add(ir.getWrite());
                            }
                        }
                        conflictRegs.forEach(sources::remove);
                        sources.addAll(toFillInSources);
                    }
                }
            }
        } while (modified);
    }

    private Map<Block, Set<Block>> analyzeNextBlocks(List<Block> blocks) {
        Map<Block, Set<Block>> nextBlockMap = new HashMap<>();
        for (Block block : blocks)
            nextBlockMap.put(block, new HashSet<>());
        for (Block block : blocks) {
            for (Pair<Block.Cond, Block> condBlock : block.getCondBlocks())
                nextBlockMap.get(block).add(condBlock.second());
            if (block.getDefaultBlock() != null)
                nextBlockMap.get(block).add(block.getDefaultBlock());
        }
        return nextBlockMap;
    }

    private Map<Block, Set<Block>> analyzePrevBlocks(List<Block> blocks) {
        Map<Block, Set<Block>> prevBlockMap = new HashMap<>();
        for (Block block : blocks)
            prevBlockMap.put(block, new HashSet<>());
        for (Block block : blocks) {
            for (Pair<Block.Cond, Block> condBlock : block.getCondBlocks())
                prevBlockMap.get(condBlock.second()).add(block);
            if (block.getDefaultBlock() != null)
                prevBlockMap.get(block.getDefaultBlock()).add(block);
        }
        return prevBlockMap;
    }
}
