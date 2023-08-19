package compile.codegen.virgen.pass;

import common.Pair;
import compile.codegen.virgen.Block;
import compile.codegen.virgen.VReg;
import compile.codegen.virgen.VirtualFunction;
import compile.codegen.virgen.vir.*;
import compile.symbol.GlobalSymbol;

import java.util.*;

public class StrongSplitRegs extends Pass {
    public StrongSplitRegs(Set<GlobalSymbol> globals, Map<String, VirtualFunction> funcs) {
        super(globals, funcs);
    }

    private static void replaceIRs(List<Block> blocks, VReg originReg, List<Pair<Set<VIR>, Set<VIR>>> w2RList) {
        Map<VIR, VReg> wMap = new HashMap<>();
        Map<VIR, VReg> rMap = new HashMap<>();
        for (Pair<Set<VIR>, Set<VIR>> r2w : w2RList) {
            VReg newReg = new VReg(originReg.getType(), originReg.getSize());
            Set<VIR> writeSet = r2w.first();
            Set<VIR> readSet = r2w.second();
            for (VIR ir : writeSet)
                wMap.put(ir, newReg);
            for (VIR ir : readSet)
                rMap.put(ir, newReg);
        }
        for (Block block : blocks) {
            for (int i = 0; i < block.size(); i++) {
                VIR ir = block.get(i);
                if (ir instanceof BinaryVIR binaryVIR) {
                    VIRItem left = binaryVIR.left;
                    if (left instanceof VReg && left == originReg)
                        left = rMap.get(ir);
                    VIRItem right = binaryVIR.right;
                    if (right instanceof VReg && right == originReg)
                        right = rMap.get(ir);
                    VReg target = binaryVIR.target;
                    if (target == originReg)
                        target = wMap.get(ir);
                    block.set(i, new BinaryVIR(binaryVIR.type, target, left, right));
                    continue;
                }
                if (ir instanceof BranchVIR branchVIR) {
                    VIRItem left = branchVIR.left;
                    if (left instanceof VReg && left == originReg)
                        left = rMap.get(ir);
                    VIRItem right = branchVIR.right;
                    if (right instanceof VReg && right == originReg)
                        right = rMap.get(ir);
                    block.set(i, new BranchVIR(branchVIR.type, left, right, branchVIR.trueBlock, branchVIR.falseBlock));
                    continue;
                }
                if (ir instanceof CallVIR callVIR) {
                    List<VIRItem> params = callVIR.params;
                    for (int j = 0; j < params.size(); j++) {
                        VIRItem param = params.get(j);
                        if (param instanceof VReg && param == originReg)
                            params.set(j, rMap.get(ir));
                    }
                    VReg target = callVIR.target;
                    if (target == originReg)
                        target = wMap.get(ir);
                    block.set(i, new CallVIR(callVIR.func, target, params));
                    continue;
                }
                if (ir instanceof LiVIR liVIR) {
                    VReg target = liVIR.target;
                    if (target == originReg)
                        target = wMap.get(ir);
                    block.set(i, new LiVIR(target, liVIR.value));
                    continue;
                }
                if (ir instanceof LoadVIR loadVIR) {
                    List<VIRItem> indexes = loadVIR.indexes;
                    for (int j = 0; j < indexes.size(); j++) {
                        VIRItem index = indexes.get(j);
                        if (index instanceof VReg && index == originReg)
                            indexes.set(j, rMap.get(ir));
                    }
                    VReg target = loadVIR.target;
                    if (target == originReg)
                        target = wMap.get(ir);
                    block.set(i, new LoadVIR(target, loadVIR.symbol, indexes));
                    continue;
                }
                if (ir instanceof MovVIR movVIR) {
                    VReg source = movVIR.source;
                    if (source == originReg)
                        source = rMap.get(ir);
                    VReg target = movVIR.target;
                    if (target == originReg)
                        target = wMap.get(ir);
                    block.set(i, new MovVIR(target, source));
                    continue;
                }
                if (ir instanceof RetVIR retVIR) {
                    VReg retVal = retVIR.retVal;
                    if (retVal == originReg)
                        retVal = rMap.get(ir);
                    block.set(i, new RetVIR(retVal));
                    continue;
                }
                if (ir instanceof StoreVIR storeVIR) {
                    List<VIRItem> indexes = storeVIR.indexes;
                    for (int j = 0; j < indexes.size(); j++) {
                        VIRItem index = indexes.get(j);
                        if (index instanceof VReg && index == originReg)
                            indexes.set(j, rMap.get(ir));
                    }
                    VReg source = storeVIR.source;
                    if (source == originReg)
                        source = rMap.get(ir);
                    block.set(i, new StoreVIR(storeVIR.symbol, indexes, source));
                    continue;
                }
                if (ir instanceof UnaryVIR unaryVIR) {
                    VIRItem source = unaryVIR.source;
                    if (source instanceof VReg && source == originReg)
                        source = rMap.get(ir);
                    VReg target = unaryVIR.target;
                    if (target == originReg)
                        target = wMap.get(ir);
                    block.set(i, new UnaryVIR(unaryVIR.type, target, source));
                    continue;
                }
            }
        }
    }

    private static Map<VIR, Set<VIR>> searchW2R(Map<Block, List<Integer>> wIRs, Map<Block, List<Integer>> rIRs,
                                                Map<Block, List<Integer>> beginIRs) {
        Map<VIR, Set<VIR>> w2RMap = new HashMap<>();
        for (Map.Entry<Block, List<Integer>> beginIRInfos : beginIRs.entrySet()) {
            Block beginBlock = beginIRInfos.getKey();
            for (int beginIndex : beginIRInfos.getValue()) {
                VIR beginIR = beginBlock.get(beginIndex);
                w2RMap.put(beginIR, new HashSet<>());
                if (rIRs.containsKey(beginBlock)) {
                    int wIndex = wIRs.get(beginBlock).indexOf(beginIndex) + 1;
                    if (wIndex < wIRs.get(beginBlock).size()) {
                        int endIndex = wIRs.get(beginBlock).get(wIndex);
                        int rIndex = 0;
                        while (rIndex < rIRs.get(beginBlock).size() && rIRs.get(beginBlock).get(rIndex) <= beginIndex)
                            rIndex++;
                        while (rIndex < rIRs.get(beginBlock).size() && rIRs.get(beginBlock).get(rIndex) <= endIndex) {
                            VIR curIR = beginBlock.get(rIRs.get(beginBlock).get(rIndex));
                            w2RMap.get(beginIR).add(curIR);
                            rIndex++;
                        }
                        continue;
                    }
                    int rIndex = 0;
                    while (rIndex < rIRs.get(beginBlock).size() && rIRs.get(beginBlock).get(rIndex) <= beginIndex)
                        rIndex++;
                    while (rIndex < rIRs.get(beginBlock).size()) {
                        VIR curIR = beginBlock.get(rIRs.get(beginBlock).get(rIndex));
                        w2RMap.get(beginIR).add(curIR);
                        rIndex++;
                    }
                }
                Set<Block> visited = new HashSet<>();
                Queue<Block> frontier = new ArrayDeque<>();
                if (beginBlock.getLast() instanceof BranchVIR branchVIR) {
                    frontier.add(branchVIR.trueBlock);
                    frontier.add(branchVIR.falseBlock);
                } else if (beginBlock.getLast() instanceof JumpVIR jumpVIR)
                    frontier.add(jumpVIR.target);
                while (!frontier.isEmpty()) {
                    Block curBlock = frontier.poll();
                    if (visited.contains(curBlock))
                        continue;
                    visited.add(curBlock);
                    if (rIRs.containsKey(curBlock)) {
                        if (wIRs.containsKey(curBlock)) {
                            int endIndex = wIRs.get(curBlock).get(0);
                            int rIndex = 0;
                            while (rIndex < rIRs.get(curBlock).size() && rIRs.get(curBlock).get(rIndex) <= endIndex) {
                                VIR curIR = curBlock.get(rIRs.get(curBlock).get(rIndex));
                                w2RMap.get(beginIR).add(curIR);
                                rIndex++;
                            }
                            continue;
                        }
                        int rIndex = 0;
                        while (rIndex < rIRs.get(curBlock).size()) {
                            VIR curIR = curBlock.get(rIRs.get(curBlock).get(rIndex));
                            w2RMap.get(beginIR).add(curIR);
                            rIndex++;
                        }
                    }
                    if (curBlock.getLast() instanceof BranchVIR branchVIR) {
                        frontier.add(branchVIR.trueBlock);
                        frontier.add(branchVIR.falseBlock);
                    } else if (curBlock.getLast() instanceof JumpVIR jumpVIR)
                        frontier.add(jumpVIR.target);
                }
            }
        }
        return w2RMap;
    }

    @Override
    public boolean run() {
        boolean modified = false;
        for (VirtualFunction func : funcs.values()) {
            List<Block> blocks = func.getBlocks();
            Map<VReg, Map<Block, List<Integer>>> wMap = new HashMap<>();
            Map<VReg, Map<Block, List<Integer>>> rMap = new HashMap<>();
            analyzeBasicInfo(blocks, wMap, rMap);
            for (Map.Entry<VReg, Map<Block, List<Integer>>> entry : wMap.entrySet()) {
                VReg originReg = entry.getKey();
                Map<Block, List<Integer>> wIRs = wMap.get(originReg);
                Map<Block, List<Integer>> rIRs = rMap.get(originReg);
                Map<Block, List<Integer>> beginIRs = entry.getValue();
                List<Pair<Set<VIR>, Set<VIR>>> w2RList = analyzeW2RInfo(wIRs, rIRs, beginIRs);
                if (w2RList.size() < 2)
                    continue;
                modified = true;
                replaceIRs(blocks, originReg, w2RList);
            }
        }
        return modified;
    }

    private List<Pair<Set<VIR>, Set<VIR>>> analyzeW2RInfo(Map<Block, List<Integer>> wIRs,
                                                          Map<Block, List<Integer>> rIRs,
                                                          Map<Block, List<Integer>> beginIRs) {
        Map<VIR, Set<VIR>> w2RMap = searchW2R(wIRs, rIRs, beginIRs);
        List<Pair<Set<VIR>, Set<VIR>>> w2RList = new ArrayList<>();
        for (Map.Entry<VIR, Set<VIR>> innerEntry : w2RMap.entrySet())
            w2RList.add(new Pair<>(new HashSet<>(List.of(innerEntry.getKey())), innerEntry.getValue()));
        for (int i = 0; i < w2RList.size(); i++) {
            boolean toContinue = false;
            for (int j = i + 1; j < w2RList.size(); j++) {
                boolean flag = false;
                for (VIR ir : w2RList.get(i).second()) {
                    if (w2RList.get(j).second().contains(ir)) {
                        flag = true;
                        break;
                    }
                }
                if (flag) {
                    w2RList.get(i).first().addAll(w2RList.get(j).first());
                    w2RList.get(i).second().addAll(w2RList.get(j).second());
                    w2RList.remove(j);
                    toContinue = true;
                    break;
                }
            }
            if (toContinue)
                i--;
        }
        return w2RList;
    }

    private void analyzeBasicInfo(List<Block> blocks, Map<VReg, Map<Block, List<Integer>>> wMap, Map<VReg, Map<Block,
            List<Integer>>> rMap) {
        for (Block block : blocks) {
            for (int i = 0; i < block.size(); i++) {
                VIR ir = block.get(i);
                for (VReg reg : ir.getRead()) {
                    if (!wMap.containsKey(reg))
                        wMap.put(reg, new HashMap<>());
                    if (!rMap.containsKey(reg))
                        rMap.put(reg, new HashMap<>());
                    if (!rMap.get(reg).containsKey(block))
                        rMap.get(reg).put(block, new ArrayList<>());
                    rMap.get(reg).get(block).add(i);
                }
                if (ir.getWrite() != null) {
                    if (!wMap.containsKey(ir.getWrite()))
                        wMap.put(ir.getWrite(), new HashMap<>());
                    if (!rMap.containsKey(ir.getWrite()))
                        rMap.put(ir.getWrite(), new HashMap<>());
                    if (!wMap.get(ir.getWrite()).containsKey(block))
                        wMap.get(ir.getWrite()).put(block, new ArrayList<>());
                    wMap.get(ir.getWrite()).get(block).add(i);
                }
            }
        }
    }
}
