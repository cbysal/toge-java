package compile.codegen.virgen.pass;

import common.Pair;
import compile.codegen.virgen.Block;
import compile.codegen.virgen.VReg;
import compile.codegen.virgen.VirtualFunction;
import compile.codegen.virgen.vir.VIR;
import compile.symbol.GlobalSymbol;

import java.util.*;
import java.util.stream.Collectors;

public class RemoveUnreachableBlocks extends Pass {
    public RemoveUnreachableBlocks(Set<GlobalSymbol> globals, Map<String, VirtualFunction> funcs) {
        super(globals, funcs);
    }

    @Override
    public boolean run() {
        boolean modified = false;
        for (VirtualFunction func : funcs.values()) {
            List<Block> blocks = func.getBlocks();
            Set<Block> reachableBlocks = analyzeReachableBlocks(blocks);
            Set<Block> unreachableBlocks = new HashSet<>(blocks);
            unreachableBlocks.removeAll(reachableBlocks);
            Set<VReg> unusedRegs = analyzeDefRegs(unreachableBlocks);
            List<Block> newBlocks = blocks.stream().filter(reachableBlocks::contains).collect(Collectors.toList());
            repairPhi(newBlocks, unusedRegs);
            modified |= func.getBlocks().size() != newBlocks.size();
            func.setBlocks(newBlocks);
        }
        return modified;
    }

    private void repairPhi(List<Block> blocks, Set<VReg> unusedRegs) {
        for (Block block : blocks)
            for (Map<VReg, Block> regsWithBlock : block.getPhiMap().values())
                unusedRegs.forEach(regsWithBlock::remove);
    }

    private Set<VReg> analyzeDefRegs(Set<Block> blocks) {
        Set<VReg> defRegs = new HashSet<>();
        for (Block block : blocks) {
            defRegs.addAll(block.getPhiMap().keySet());
            for (VIR ir : block)
                if (ir.getWrite() != null)
                    defRegs.add(ir.getWrite());
        }
        return defRegs;
    }

    private Set<Block> analyzeReachableBlocks(List<Block> blocks) {
        Queue<Block> frontier = new ArrayDeque<>();
        frontier.offer(blocks.get(0));
        Set<Block> reachableBlocks = new HashSet<>();
        while (!frontier.isEmpty()) {
            Block curBlock = frontier.poll();
            if (reachableBlocks.contains(curBlock))
                continue;
            reachableBlocks.add(curBlock);
            if (curBlock.getDefaultBlock() != null)
                frontier.offer(curBlock.getDefaultBlock());
            curBlock.getCondBlocks().stream().map(Pair::second).filter(Objects::nonNull).forEach(frontier::offer);
        }
        return reachableBlocks;
    }
}
