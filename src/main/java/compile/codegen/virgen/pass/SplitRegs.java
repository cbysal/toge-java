package compile.codegen.virgen.pass;

import common.Pair;
import compile.codegen.virgen.Block;
import compile.codegen.virgen.VReg;
import compile.codegen.virgen.VirtualFunction;
import compile.codegen.virgen.vir.*;
import compile.symbol.GlobalSymbol;

import java.util.*;
import java.util.stream.Collectors;

public class SplitRegs extends Pass {
    public SplitRegs(Set<GlobalSymbol> globals, Map<String, VirtualFunction> funcs) {
        super(globals, funcs);
    }

    @Override
    public boolean run() {
        boolean modified = false;
        for (VirtualFunction func : funcs.values()) {
            List<Block> blocks = func.getBlocks();
            Map<VReg, Set<Pair<Block, Integer>>> wMap = new HashMap<>();
            Map<VReg, Set<Pair<Block, Integer>>> rMap = new HashMap<>();
            analyzeBasicInfo(blocks, wMap, rMap);
            for (Map.Entry<VReg, Set<Pair<Block, Integer>>> entry : wMap.entrySet()) {
                VReg originReg = entry.getKey();
                Set<Pair<Block, Integer>> beginIRs = entry.getValue();
                List<Pair<Set<VIR>, Set<VIR>>> w2RList = analyzeW2RInfo(wMap, rMap, originReg, beginIRs);
                if (w2RList.size() < 2)
                    continue;
                modified = true;
                replaceIRs(blocks, originReg, w2RList);
            }
        }
        return modified;
    }

    private static void replaceIRs(List<Block> blocks, VReg originReg, List<Pair<Set<VIR>, Set<VIR>>> w2RList) {
        for (Pair<Set<VIR>, Set<VIR>> r2w : w2RList) {
            VReg newReg = new VReg(originReg.getType(), originReg.getSize());
            Set<VIR> writeSet = r2w.first();
            Set<VIR> readSet = r2w.second();
            for (Block block : blocks) {
                for (int i = 0; i < block.size(); i++) {
                    VIR ir = block.get(i);
                    if (readSet.contains(ir)) {
                        if (ir instanceof BinaryVIR binaryVIR) {
                            VIRItem left = binaryVIR.left();
                            if (left instanceof VReg && left == originReg)
                                left = newReg;
                            VIRItem right = binaryVIR.right();
                            if (right instanceof VReg && right == originReg)
                                right = newReg;
                            block.set(i, new BinaryVIR(binaryVIR.type(), binaryVIR.target(), left, right));
                            continue;
                        }
                        if (ir instanceof BranchVIR branchVIR) {
                            VIRItem left = branchVIR.left();
                            if (left instanceof VReg && left == originReg)
                                left = newReg;
                            VIRItem right = branchVIR.right();
                            if (right instanceof VReg && right == originReg)
                                right = newReg;
                            block.set(i, new BranchVIR(branchVIR.type(), left, right, branchVIR.trueBlock(),
                                    branchVIR.falseBlock()));
                            continue;
                        }
                        if (ir instanceof CallVIR callVIR) {
                            List<VIRItem> params = callVIR.params();
                            for (int j = 0; j < params.size(); j++) {
                                VIRItem param = params.get(j);
                                if (param instanceof VReg && param == originReg)
                                    params.set(j, newReg);
                            }
                            block.set(i, new CallVIR(callVIR.func(), callVIR.target(), params));
                            continue;
                        }
                        if (ir instanceof LoadVIR loadVIR) {
                            List<VIRItem> indexes = loadVIR.indexes();
                            for (int j = 0; j < indexes.size(); j++) {
                                VIRItem index = indexes.get(j);
                                if (index instanceof VReg && index == originReg)
                                    indexes.set(j, newReg);
                            }
                            block.set(i, new LoadVIR(loadVIR.target(), loadVIR.symbol(), indexes));
                            continue;
                        }
                        if (ir instanceof MovVIR movVIR) {
                            VReg source = movVIR.source();
                            if (source == originReg)
                                source = newReg;
                            block.set(i, new MovVIR(movVIR.target(), source));
                            continue;
                        }
                        if (ir instanceof RetVIR retVIR) {
                            VReg retVal = retVIR.retVal();
                            if (retVal == originReg)
                                retVal = newReg;
                            block.set(i, new RetVIR(retVal));
                            continue;
                        }
                        if (ir instanceof StoreVIR storeVIR) {
                            List<VIRItem> indexes = storeVIR.indexes();
                            for (int j = 0; j < indexes.size(); j++) {
                                VIRItem index = indexes.get(j);
                                if (index instanceof VReg && index == originReg)
                                    indexes.set(j, newReg);
                            }
                            VReg source = storeVIR.source();
                            if (source == originReg)
                                source = newReg;
                            block.set(i, new StoreVIR(storeVIR.symbol(), indexes, source));
                            continue;
                        }
                        if (ir instanceof UnaryVIR unaryVIR) {
                            VIRItem source = unaryVIR.source();
                            if (source instanceof VReg && source == originReg)
                                source = newReg;
                            block.set(i, new UnaryVIR(unaryVIR.type(), unaryVIR.target(), source));
                            continue;
                        }
                    }
                }
            }
            for (Block block : blocks) {
                for (int i = 0; i < block.size(); i++) {
                    VIR ir = block.get(i);
                    if (writeSet.contains(ir)) {
                        if (ir instanceof BinaryVIR binaryVIR) {
                            VReg target = binaryVIR.target();
                            if (target == originReg)
                                target = newReg;
                            block.set(i, new BinaryVIR(binaryVIR.type(), target, binaryVIR.left(), binaryVIR.right()));
                            continue;
                        }
                        if (ir instanceof CallVIR callVIR) {
                            VReg target = callVIR.target();
                            if (target == originReg)
                                target = newReg;
                            block.set(i, new CallVIR(callVIR.func(), target, callVIR.params()));
                            continue;
                        }
                        if (ir instanceof LiVIR liVIR) {
                            VReg target = liVIR.target();
                            if (target == originReg)
                                target = newReg;
                            block.set(i, new LiVIR(target, liVIR.value()));
                            continue;
                        }
                        if (ir instanceof LoadVIR loadVIR) {
                            VReg target = loadVIR.target();
                            if (target == originReg)
                                target = newReg;
                            block.set(i, new LoadVIR(target, loadVIR.symbol(), loadVIR.indexes()));
                            continue;
                        }
                        if (ir instanceof MovVIR movVIR) {
                            VReg target = movVIR.target();
                            if (target == originReg)
                                target = newReg;
                            block.set(i, new MovVIR(target, movVIR.source()));
                            continue;
                        }
                        if (ir instanceof UnaryVIR unaryVIR) {
                            VReg target = unaryVIR.target();
                            if (target == originReg)
                                target = newReg;
                            block.set(i, new UnaryVIR(unaryVIR.type(), target, unaryVIR.source()));
                            continue;
                        }
                    }
                }
            }
        }
    }

    private List<Pair<Set<VIR>, Set<VIR>>> analyzeW2RInfo(Map<VReg, Set<Pair<Block, Integer>>> wMap, Map<VReg,
            Set<Pair<Block, Integer>>> rMap, VReg originReg, Set<Pair<Block, Integer>> beginIRs) {
        Set<VIR> wSet = wMap.get(originReg).stream().map(p -> p.first().get(p.second())).collect(Collectors.toSet());
        Set<VIR> rSet = rMap.get(originReg).stream().map(p -> p.first().get(p.second())).collect(Collectors.toSet());
        Map<VIR, Set<VIR>> w2RMap = searchW2R(wSet, rSet, beginIRs);
        List<Pair<Set<VIR>, Set<VIR>>> w2RList = new ArrayList<>();
        for (Map.Entry<VIR, Set<VIR>> innerEntry : w2RMap.entrySet())
            w2RList.add(new Pair<>(new HashSet<>(List.of(innerEntry.getKey())), innerEntry.getValue()));
        int sizeBefore, sizeAfter;
        do {
            sizeBefore = w2RList.size();
            for (int i = 0; i < w2RList.size(); i++) {
                for (int j = i + 1; j < w2RList.size(); j++) {
                    Set<VIR> commonIRs = new HashSet<>(w2RList.get(i).second());
                    commonIRs.retainAll(w2RList.get(j).second());
                    if (!commonIRs.isEmpty()) {
                        w2RList.get(i).first().addAll(w2RList.get(j).first());
                        w2RList.get(i).second().addAll(w2RList.get(j).second());
                        w2RList.remove(j);
                        j--;
                    }
                }
            }
            sizeAfter = w2RList.size();
        } while (sizeBefore != sizeAfter);
        return w2RList;
    }

    private static Map<VIR, Set<VIR>> searchW2R(Set<VIR> wSet, Set<VIR> rSet, Set<Pair<Block, Integer>> beginIRs) {
        Map<VIR, Set<VIR>> w2RMap = new HashMap<>();
        for (Pair<Block, Integer> beginIRInfo : beginIRs) {
            Block beginBlock = beginIRInfo.first();
            int beginIndex = beginIRInfo.second();
            VIR beginIR = beginBlock.get(beginIndex);
            w2RMap.put(beginIR, new HashSet<>());
            boolean isEnd = false;
            for (int i = beginIndex + 1; i < beginBlock.size(); i++) {
                VIR curIR = beginBlock.get(i);
                if (rSet.contains(curIR))
                    w2RMap.get(beginIR).add(curIR);
                if (wSet.contains(curIR)) {
                    isEnd = true;
                    break;
                }
            }
            if (isEnd)
                continue;
            Set<Block> visited = new HashSet<>();
            Queue<Block> frontier = new ArrayDeque<>();
            if (beginBlock.getLast() instanceof BranchVIR branchVIR) {
                frontier.add(branchVIR.trueBlock());
                frontier.add(branchVIR.falseBlock());
            } else if (beginBlock.getLast() instanceof JumpVIR jumpVIR)
                frontier.add(jumpVIR.target());
            while (!frontier.isEmpty()) {
                Block curBlock = frontier.poll();
                if (visited.contains(curBlock))
                    continue;
                visited.add(curBlock);
                isEnd = false;
                for (VIR curIR : curBlock) {
                    if (rSet.contains(curIR))
                        w2RMap.get(beginIR).add(curIR);
                    if (wSet.contains(curIR)) {
                        isEnd = true;
                        break;
                    }
                }
                if (isEnd)
                    continue;
                if (curBlock.getLast() instanceof BranchVIR branchVIR) {
                    frontier.add(branchVIR.trueBlock());
                    frontier.add(branchVIR.falseBlock());
                } else if (curBlock.getLast() instanceof JumpVIR jumpVIR)
                    frontier.add(jumpVIR.target());
            }
        }
        return w2RMap;
    }

    private void analyzeBasicInfo(List<Block> blocks, Map<VReg, Set<Pair<Block, Integer>>> wMap, Map<VReg,
            Set<Pair<Block, Integer>>> rMap) {
        for (Block block : blocks) {
            for (int i = 0; i < block.size(); i++) {
                VIR ir = block.get(i);
                for (VReg reg : ir.getRead()) {
                    if (!wMap.containsKey(reg))
                        wMap.put(reg, new HashSet<>());
                    if (!rMap.containsKey(reg))
                        rMap.put(reg, new HashSet<>());
                    rMap.get(reg).add(new Pair<>(block, i));
                }
                if (ir.getWrite() != null) {
                    if (!wMap.containsKey(ir.getWrite()))
                        wMap.put(ir.getWrite(), new HashSet<>());
                    if (!rMap.containsKey(ir.getWrite()))
                        rMap.put(ir.getWrite(), new HashSet<>());
                    wMap.get(ir.getWrite()).add(new Pair<>(block, i));
                }
            }
        }
    }
}
