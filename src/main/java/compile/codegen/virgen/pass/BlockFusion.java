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
        for (Map<VReg, Block> regsWithBlock : block2.getPhiMap().values())
            for (VReg reg : regsWithBlock.keySet())
                if (defRegs.contains(reg))
                    return false;
        return true;
    }

    private void mergeBlocks(List<Block> blocks, Block curBlock, Block nextBlock) {
        Map<VReg, Map<VReg, Block>> curPhiMap = curBlock.getPhiMap();
        Map<VReg, Map<VReg, Block>> nextPhiMap = nextBlock.getPhiMap();
        for (Map.Entry<VReg, Map<VReg, Block>> entry : nextPhiMap.entrySet()) {
            VReg target = entry.getKey();
            Map<VReg, Block> regsWithBlock = entry.getValue();
            if (curPhiMap.containsKey(target))
                curPhiMap.get(target).putAll(regsWithBlock);
            else
                curPhiMap.put(target, regsWithBlock);
        }
        curBlock.addAll(nextBlock);
        curBlock.clearCondBlocks();
        nextBlock.getCondBlocks().forEach(curBlock::setCondBlock);
        curBlock.setDefaultBlock(nextBlock.getDefaultBlock());
        blocks.remove(nextBlock);
        Set<VReg> defRegs = new HashSet<>(nextPhiMap.keySet());
        for (VIR ir : nextBlock)
            if (ir.getWrite() != null)
                defRegs.add(ir.getWrite());
        for (Block block : blocks) {
            Map<VReg, Map<VReg, Block>> phiMap = block.getPhiMap();
            for (Map<VReg, Block> regsWithBlock : phiMap.values())
                for (VReg defReg : defRegs)
                    if (regsWithBlock.containsKey(defReg))
                        regsWithBlock.put(defReg, curBlock);
        }
        for (VirtualFunction func : funcs.values())
            removePhiConflict(func);
    }

    private void removePhiConflict(VirtualFunction func) {
        boolean modified;
        do {
            modified = false;
            for (Block curBlock : func.getBlocks()) {
                for (Map<VReg, Block> regsWithBlock : curBlock.getPhiMap().values()) {
                    Map<Block, Integer> counter = new HashMap<>();
                    for (Block block : regsWithBlock.values())
                        counter.put(block, counter.getOrDefault(block, 0) + 1);
                    List<Block> toProcessBlocks =
                            counter.keySet().stream().filter(block -> counter.get(block) > 1).toList();
                    modified |= !toProcessBlocks.isEmpty();
                    for (Block toProcessBlock : toProcessBlocks) {
                        Set<VReg> conflictRegs = new HashSet<>();
                        for (Map.Entry<VReg, Block> regWithBlock : regsWithBlock.entrySet())
                            if (regWithBlock.getValue() == toProcessBlock)
                                conflictRegs.add(regWithBlock.getKey());
                        Map<VReg, Block> toFillInRegWithBlockMap = new HashMap<>();
                        for (Map.Entry<VReg, Map<VReg, Block>> entry : toProcessBlock.getPhiMap().entrySet())
                            if (conflictRegs.contains(entry.getKey()))
                                toFillInRegWithBlockMap.putAll(entry.getValue());
                        for (VIR ir : toProcessBlock) {
                            if (ir.getWrite() != null && conflictRegs.contains(ir.getWrite())) {
                                toFillInRegWithBlockMap.clear();
                                toFillInRegWithBlockMap.put(ir.getWrite(), toProcessBlock);
                            }
                        }
                        conflictRegs.forEach(regsWithBlock::remove);
                        regsWithBlock.putAll(toFillInRegWithBlockMap);
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
