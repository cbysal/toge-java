package compile.codegen.virgen.pass;

import compile.codegen.virgen.Block;
import compile.codegen.virgen.VirtualFunction;
import compile.codegen.virgen.vir.BranchVIR;
import compile.codegen.virgen.vir.JumpVIR;
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
            List<Block> newBlocks = blocks.stream().filter(reachableBlocks::contains).collect(Collectors.toList());
            modified |= func.getBlocks().size() != newBlocks.size();
            func.setBlocks(newBlocks);
        }
        return modified;
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
            VIR lastIR = curBlock.get(curBlock.size() - 1);
            if (lastIR instanceof BranchVIR branchVIR) {
                frontier.offer(branchVIR.trueBlock);
                frontier.offer(branchVIR.falseBlock);
            } else if (lastIR instanceof JumpVIR jumpVIR)
                frontier.offer(jumpVIR.target);
        }
        return reachableBlocks;
    }
}
