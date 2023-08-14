package compile.codegen.virgen.pass;

import common.Pair;
import compile.codegen.virgen.Block;
import compile.codegen.virgen.VReg;
import compile.codegen.virgen.VirtualFunction;
import compile.codegen.virgen.vir.*;
import compile.symbol.GlobalSymbol;

import java.util.*;
import java.util.stream.Collectors;

public class ConstructSSA extends Pass {
    private static class Renamer {
        private final Map<VReg, Stack<VReg>> renameStackMap = new HashMap<>();

        public VReg newName(VReg reg) {
            if (!renameStackMap.containsKey(reg))
                renameStackMap.put(reg, new Stack<>());
            VReg newReg = new VReg(reg.getType(), reg.getSize());
            renameStackMap.get(reg).push(newReg);
            return newReg;
        }

        public VReg top(VReg reg) {
            if (!renameStackMap.containsKey(reg))
                return null;
            return renameStackMap.get(reg).peek();
        }

        public Renamer clone() {
            Renamer newRenamer = new Renamer();
            for (Map.Entry<VReg, Stack<VReg>> entry : renameStackMap.entrySet()) {
                newRenamer.renameStackMap.put(entry.getKey(), new Stack<>());
                for (VReg reg : entry.getValue())
                    newRenamer.renameStackMap.get(entry.getKey()).push(reg);
            }
            return newRenamer;
        }
    }

    public ConstructSSA(Set<GlobalSymbol> globals, Map<String, VirtualFunction> funcs) {
        super(globals, funcs);
    }

    @Override
    public boolean run() {
        for (VirtualFunction func : funcs.values()) {
            Set<VReg> todoRegs = analyzeTodoRegs(func);
            if (todoRegs.isEmpty())
                continue;
            insertPhi(func, todoRegs);
            renameVars(func, todoRegs);
        }
        return false;
    }

    private Set<VReg> analyzeTodoRegs(VirtualFunction func) {
        List<Block> blocks = func.getBlocks();
        Map<VReg, Integer> defCounter = new HashMap<>();
        for (Block block : blocks) {
            for (VIR ir : block) {
                VReg def = ir.getWrite();
                if (def != null)
                    defCounter.put(def, defCounter.getOrDefault(def, 0) + 1);
            }
        }
        return defCounter.entrySet().stream().filter(entry -> entry.getValue() > 1).map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    private Map<Block, Set<Block>> analyzePrevBlocks(VirtualFunction func) {
        List<Block> blocks = func.getBlocks();
        Map<Block, Set<Block>> prevBlocks = new HashMap<>();
        blocks.forEach(block -> prevBlocks.put(block, new HashSet<>()));
        for (Block curBlock : blocks) {
            curBlock.getCondBlocks().stream().map(Pair::second).forEach(nextBlock -> prevBlocks.get(nextBlock).add(curBlock));
            if (curBlock.getDefaultBlock() != null)
                prevBlocks.get(curBlock.getDefaultBlock()).add(curBlock);
        }
        return prevBlocks;
    }

    private Map<Block, Set<Block>> analyzeDominance(VirtualFunction func) {
        Map<Block, Set<Block>> prevBlockMap = analyzePrevBlocks(func);
        Map<Block, Set<Block>> domMap = new HashMap<>();
        List<Block> blocks = func.getBlocks();
        for (Block block : blocks)
            domMap.put(block, new HashSet<>(blocks));
        boolean toContinue;
        do {
            toContinue = false;
            for (Block curBlock : blocks) {
                Set<Block> prevBlocks = prevBlockMap.get(curBlock);
                Set<Block> newDom = new HashSet<>();
                boolean isFirst = true;
                for (Block prevBlock : prevBlocks) {
                    if (isFirst) {
                        newDom.addAll(domMap.get(prevBlock));
                        isFirst = false;
                    } else {
                        newDom.retainAll(domMap.get(prevBlock));
                    }
                }
                newDom.add(curBlock);
                if (!newDom.equals(domMap.get(curBlock))) {
                    domMap.put(curBlock, newDom);
                    toContinue = true;
                }
            }
        } while (toContinue);
        return domMap;
    }

    private Map<Block, Block> analyzeIDom(VirtualFunction func) {
        List<Block> blocks = func.getBlocks();
        Map<Block, Set<Block>> domMap = analyzeDominance(func);
        for (Block block : blocks)
            domMap.get(block).remove(block);
        Map<Block, Block> idomMap = new HashMap<>();
        while (!domMap.isEmpty()) {
            Iterator<Map.Entry<Block, Set<Block>>> iterator = domMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Block, Set<Block>> entry = iterator.next();
                if (entry.getValue().isEmpty()) {
                    idomMap.put(entry.getKey(), null);
                    iterator.remove();
                } else if (entry.getValue().size() == 1) {
                    Block key = entry.getKey();
                    Block value = entry.getValue().iterator().next();
                    idomMap.put(key, value);
                    for (Set<Block> doms : domMap.values())
                        if (doms.contains(key))
                            doms.remove(value);
                    iterator.remove();
                }
            }
        }
        return idomMap;
    }

    private Map<Block, Set<Block>> analyzeDomFrontier(VirtualFunction func) {
        Map<Block, Set<Block>> prevBlockMap = analyzePrevBlocks(func);
        Map<Block, Block> idomMap = analyzeIDom(func);
        Map<Block, Set<Block>> domFrontierMap = new HashMap<>();
        List<Block> blocks = func.getBlocks();
        for (Block block : blocks)
            domFrontierMap.put(block, new HashSet<>());
        for (Block block : blocks) {
            Set<Block> prevBlocks = prevBlockMap.get(block);
            if (prevBlocks.size() >= 2) {
                for (Block prevBlock : prevBlocks) {
                    Block runner = prevBlock;
                    while (runner != idomMap.get(block)) {
                        domFrontierMap.get(runner).add(block);
                        runner = idomMap.get(runner);
                    }
                }
            }
        }
        return domFrontierMap;
    }

    private void insertPhi(VirtualFunction func, Set<VReg> todoRegs) {
        Map<VReg, Set<Block>> todoBlockMap = new HashMap<>();
        for (VReg reg : todoRegs)
            todoBlockMap.put(reg, new HashSet<>());
        for (Block block : func.getBlocks())
            for (VIR ir : block)
                if (todoRegs.contains(ir.getWrite()))
                    todoBlockMap.get(ir.getWrite()).add(block);
        Map<Block, Set<Block>> domFrontierMap = analyzeDomFrontier(func);
        for (Map.Entry<VReg, Set<Block>> entry : todoBlockMap.entrySet()) {
            VReg todoReg = entry.getKey();
            Queue<Block> queue = new ArrayDeque<>(entry.getValue());
            while (!queue.isEmpty()) {
                Block block = queue.poll();
                for (Block frontier : domFrontierMap.get(block)) {
                    if (!frontier.getPhiMap().containsKey(todoReg)) {
                        frontier.getPhiMap().put(todoReg, new HashMap<>());
                        queue.offer(frontier);
                    }
                }
            }
        }
    }

    private void renameVars(VirtualFunction func, Set<VReg> todoRegs) {
        Map<Block, Block> iDomMap = analyzeIDom(func);
        List<Block> blocks = func.getBlocks();
        Map<Block, Set<Block>> domTree = new HashMap<>();
        for (Block block : blocks)
            domTree.put(block, new HashSet<>());
        for (Map.Entry<Block, Block> entry : iDomMap.entrySet()) {
            Block from = entry.getValue();
            Block to = entry.getKey();
            if (from != null)
                domTree.get(from).add(to);
        }
        Renamer renamer = new Renamer();
        Map<VReg, VReg> newToOldMap = new HashMap<>();
        for (VReg reg : todoRegs)
            newToOldMap.put(reg, reg);
        Map<VReg, Block> regToBlockMap = new HashMap<>();
        for (Block block : blocks) {
            for (VReg reg : block.getPhiMap().keySet())
                regToBlockMap.put(reg, block);
            for (VIR ir : block)
                if (todoRegs.contains(ir.getWrite()))
                    regToBlockMap.put(ir.getWrite(), block);
        }
        renameVarsHelper(domTree, todoRegs, renamer, newToOldMap, regToBlockMap, func.getBlocks().get(0));
    }

    private void renameVarsHelper(Map<Block, Set<Block>> domTree, Set<VReg> todoRegs, Renamer renamer, Map<VReg,
            VReg> newToOldMap, Map<VReg, Block> regToBlockMap, Block block) {
        Map<VReg, Map<VReg, Block>> phiMap = block.getPhiMap();
        Map<VReg, Map<VReg, Block>> newPhiMap = new HashMap<>();
        for (Map.Entry<VReg, Map<VReg, Block>> entry : phiMap.entrySet()) {
            Map<VReg, Block> oldSources = entry.getValue();
            Map<VReg, Block> newSources = new HashMap<>();
            for (Map.Entry<VReg, Block> oldSource : oldSources.entrySet()) {
                VReg newSource = oldSource.getKey();
                if (todoRegs.contains(newSource)) {
                    newSource = renamer.top(newSource);
                    newSources.put(newSource, regToBlockMap.get(newSource));
                } else {
                    newSources.put(oldSource.getKey(), oldSource.getValue());
                }
            }
            VReg oldTarget = entry.getKey();
            VReg newTarget = oldTarget;
            if (todoRegs.contains(oldTarget))
                newTarget = renamer.newName(newTarget);
            newToOldMap.put(newTarget, oldTarget);
            newPhiMap.put(newTarget, newSources);
            regToBlockMap.put(newTarget, block);
        }
        phiMap.clear();
        phiMap.putAll(newPhiMap);
        for (int i = 0; i < block.size(); i++) {
            VIR ir = block.get(i);
            if (ir instanceof BinaryVIR binaryVIR) {
                VIRItem newLeft = binaryVIR.left();
                VIRItem newRight = binaryVIR.right();
                if (newLeft instanceof VReg reg && todoRegs.contains(newLeft))
                    newLeft = renamer.top(reg);
                if (newRight instanceof VReg reg && todoRegs.contains(newRight))
                    newRight = renamer.top(reg);
                VReg newTarget = binaryVIR.target();
                if (todoRegs.contains(newTarget))
                    newTarget = renamer.newName(newTarget);
                block.set(i, new BinaryVIR(binaryVIR.type(), newTarget, newLeft, newRight));
                regToBlockMap.put(newTarget, block);
                continue;
            }
            if (ir instanceof CallVIR callVIR) {
                List<VIRItem> newParams = callVIR.params();
                for (int j = 0; j < newParams.size(); j++) {
                    VIRItem newParam = newParams.get(j);
                    if (newParam instanceof VReg reg && todoRegs.contains(reg))
                        newParam = renamer.top(reg);
                    newParams.set(j, newParam);
                }
                VReg newTarget = callVIR.target();
                if (newTarget != null && todoRegs.contains(newTarget))
                    newTarget = renamer.newName(newTarget);
                block.set(i, new CallVIR(callVIR.func(), newTarget, newParams));
                if (newTarget != null)
                    regToBlockMap.put(newTarget, block);
                continue;
            }
            if (ir instanceof LiVIR liVIR) {
                VReg newTarget = liVIR.target();
                if (todoRegs.contains(newTarget))
                    newTarget = renamer.newName(newTarget);
                block.set(i, new LiVIR(newTarget, liVIR.value()));
                regToBlockMap.put(newTarget, block);
                continue;
            }
            if (ir instanceof LoadVIR loadVIR) {
                List<VIRItem> newIndexes = loadVIR.indexes();
                for (int j = 0; j < newIndexes.size(); j++) {
                    VIRItem newIndex = newIndexes.get(j);
                    if (newIndex instanceof VReg reg && todoRegs.contains(reg))
                        newIndex = renamer.top(reg);
                    newIndexes.set(j, newIndex);
                }
                VReg newTarget = loadVIR.target();
                if (todoRegs.contains(newTarget))
                    newTarget = renamer.newName(newTarget);
                block.set(i, new LoadVIR(newTarget, loadVIR.symbol(), newIndexes));
                regToBlockMap.put(newTarget, block);
                continue;
            }
            if (ir instanceof MovVIR movVIR) {
                VReg newSource = movVIR.source();
                if (todoRegs.contains(newSource))
                    newSource = renamer.top(newSource);
                VReg newTarget = movVIR.target();
                if (todoRegs.contains(newTarget))
                    newTarget = renamer.newName(newTarget);
                block.set(i, new MovVIR(newTarget, newSource));
                regToBlockMap.put(newTarget, block);
                continue;
            }
            if (ir instanceof RetVIR retVIR) {
                VReg newRetVal = retVIR.retVal();
                if (todoRegs.contains(newRetVal))
                    newRetVal = renamer.top(newRetVal);
                block.set(i, new RetVIR(newRetVal));
                continue;
            }
            if (ir instanceof StoreVIR storeVIR) {
                List<VIRItem> newIndexes = storeVIR.indexes();
                for (int j = 0; j < newIndexes.size(); j++) {
                    VIRItem newIndex = newIndexes.get(j);
                    if (newIndex instanceof VReg reg && todoRegs.contains(reg))
                        newIndex = renamer.top(reg);
                    newIndexes.set(j, newIndex);
                }
                VReg newSource = storeVIR.source();
                if (todoRegs.contains(newSource))
                    newSource = renamer.top(newSource);
                block.set(i, new StoreVIR(storeVIR.symbol(), newIndexes, newSource));
                continue;
            }
            if (ir instanceof UnaryVIR unaryVIR) {
                VIRItem newSource = unaryVIR.source();
                if (newSource instanceof VReg reg && todoRegs.contains(reg))
                    newSource = renamer.top(reg);
                VReg newTarget = unaryVIR.target();
                if (todoRegs.contains(newTarget))
                    newTarget = renamer.newName(newTarget);
                block.set(i, new UnaryVIR(unaryVIR.type(), newTarget, newSource));
                regToBlockMap.put(newTarget, block);
                continue;
            }
        }
        List<Pair<Block.Cond, Block>> condBlocks = block.getCondBlocks();
        for (int i = 0; i < condBlocks.size(); i++) {
            Pair<Block.Cond, Block> condBlock = condBlocks.get(i);
            Block.Cond cond = condBlock.first();
            Block targetBlock = condBlock.second();
            VIRItem newLeft = cond.left();
            VIRItem newRight = cond.right();
            if (newLeft instanceof VReg reg && todoRegs.contains(reg))
                newLeft = renamer.top(reg);
            if (newRight instanceof VReg reg && todoRegs.contains(reg))
                newRight = renamer.top(reg);
            Block.Cond newCond = new Block.Cond(cond.type(), newLeft, newRight);
            condBlocks.set(i, new Pair<>(newCond, targetBlock));
        }
        List<Block> nextBlocks = new ArrayList<>(block.getCondBlocks().stream().map(Pair::second).toList());
        if (block.getDefaultBlock() != null)
            nextBlocks.add(block.getDefaultBlock());
        for (Block nextBlock : nextBlocks) {
            Map<VReg, Map<VReg, Block>> nextPhiMap = nextBlock.getPhiMap();
            for (Map.Entry<VReg, Map<VReg, Block>> entry : nextPhiMap.entrySet()) {
                VReg reg = entry.getKey();
                Map<VReg, Block> regsWithBlock = entry.getValue();
                VReg newReg = renamer.top(newToOldMap.get(reg));
                if (newReg != null)
                    regsWithBlock.put(newReg, regToBlockMap.get(newReg));
            }
        }
        for (Block domBlock : domTree.get(block))
            renameVarsHelper(domTree, todoRegs, renamer.clone(), newToOldMap, regToBlockMap, domBlock);
    }
}
