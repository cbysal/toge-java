package compile.llvm.pass.analysis;

import com.google.common.collect.Sets;
import compile.llvm.BasicBlock;
import compile.llvm.Function;
import compile.llvm.ir.BranchInst;
import compile.llvm.ir.Instruction;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

public final class DominatorTreeAnalysis implements Analysis<Map<BasicBlock, Set<BasicBlock>>> {
    private final Function function;
    private final Map<BasicBlock, Set<BasicBlock>> prevMap = new HashMap<>();
    private final Map<BasicBlock, Set<BasicBlock>> domMap = new HashMap<>();
    private final Map<BasicBlock, BasicBlock> iDomMap = new HashMap<>();
    private final Map<BasicBlock, Set<BasicBlock>> domTree = new HashMap<>();
    private final Map<BasicBlock, Set<BasicBlock>> domFrontier = new HashMap<>();

    public DominatorTreeAnalysis(Function function) {
        this.function = function;
    }

    private void analyzePrev() {
        if (!prevMap.isEmpty())
            return;
        for (BasicBlock block : function)
            prevMap.put(block, new HashSet<>());
        for (BasicBlock block : function) {
            for (Instruction inst : block) {
                if (inst instanceof BranchInst branchInst) {
                    if (branchInst.isConditional()) {
                        BasicBlock trueBlock = branchInst.getOperand(1);
                        BasicBlock falseBlock = branchInst.getOperand(2);
                        prevMap.get(trueBlock).add(block);
                        prevMap.get(falseBlock).add(block);
                    } else {
                        BasicBlock destBlock = branchInst.getOperand(0);
                        prevMap.get(destBlock).add(block);
                    }
                }
            }
        }
    }

    private void analyzeDom() {
        if (!domMap.isEmpty())
            return;
        for (BasicBlock block : function)
            domMap.put(block, new HashSet<>(function.stream().toList()));
        boolean changed;
        do {
            changed = false;
            for (BasicBlock block : function) {
                Set<BasicBlock> newSet = new HashSet<>(prevMap.get(block).stream().map(domMap::get).reduce(Sets::intersection).orElse(new HashSet<>()));
                newSet.add(block);
                if (!newSet.equals(domMap.get(block))) {
                    domMap.put(block, newSet);
                    changed = true;
                }
            }
        } while (changed);
    }

    private void analyzeIdom() {
        if (!iDomMap.isEmpty())
            return;
        for (BasicBlock block : function) {
            domMap.get(block).remove(block);
            if (domMap.get(block).isEmpty())
                iDomMap.put(block, null);
            else if (domMap.get(block).size() == 1)
                iDomMap.put(block, domMap.get(block).iterator().next());
        }
        while (iDomMap.size() < function.size()) {
            for (BasicBlock block : function) {
                if (iDomMap.containsKey(block))
                    continue;
                for (Map.Entry<BasicBlock, BasicBlock> entry : iDomMap.entrySet())
                    if (domMap.get(block).contains(entry.getKey()))
                        domMap.get(block).remove(entry.getValue());
            }
            for (BasicBlock block : function) {
                if (iDomMap.containsKey(block))
                    continue;
                if (domMap.get(block).size() == 1)
                    iDomMap.put(block, domMap.get(block).iterator().next());
            }
        }
    }

    private void analyzeDomTree() {
        if (!domTree.isEmpty())
            return;
        for (Map.Entry<BasicBlock, BasicBlock> entry : iDomMap.entrySet()) {
            BasicBlock from = entry.getValue();
            BasicBlock to = entry.getKey();
            if (from != null)
                domTree.computeIfAbsent(from, v -> new HashSet<>()).add(to);
        }
    }

    private void analyzeDF() {
        if (!domFrontier.isEmpty())
            return;
        for (BasicBlock block : function)
            domFrontier.put(block, new HashSet<>());
        for (BasicBlock block : function) {
            if (prevMap.get(block).size() >= 2) {
                for (BasicBlock prevBlock : prevMap.get(block)) {
                    BasicBlock runner = prevBlock;
                    while (runner != iDomMap.get(block)) {
                        domFrontier.get(runner).add(block);
                        runner = iDomMap.get(runner);
                    }
                }
            }
        }
    }

    public Map<BasicBlock, BasicBlock> getIDom() {
        analyzePrev();
        analyzeDom();
        analyzeIdom();
        return iDomMap;
    }

    public Map<BasicBlock, Set<BasicBlock>> getDomTree() {
        analyzePrev();
        analyzeDom();
        analyzeIdom();
        analyzeDomTree();
        return domTree;
    }

    public Map<BasicBlock, Set<BasicBlock>> getDomFrontier() {
        analyzePrev();
        analyzeDom();
        analyzeIdom();
        analyzeDF();
        return domFrontier;
    }

    @Override
    public Map<BasicBlock, Set<BasicBlock>> getResult() {
        analyzePrev();
        analyzeDom();
        analyzeIdom();
        analyzeDF();
        return domFrontier;
    }
}
