package compile.llvm.pass;

import compile.llvm.BasicBlock;
import compile.llvm.Function;
import compile.llvm.Module;
import compile.llvm.contant.ConstantNumber;
import compile.llvm.ir.*;
import compile.llvm.pass.analysis.DomTreeAnalysis;
import compile.llvm.type.BasicType;
import compile.llvm.type.PointerType;
import compile.llvm.value.Use;
import compile.llvm.value.Value;

import java.util.*;

public class PromotePass extends FunctionPass {
    private final Map<BasicBlock, Map<AllocaInst, PHINode>> globalPhiMap = new HashMap<>();

    public PromotePass(Module module) {
        super(module);
    }

    private Set<AllocaInst> analyzePromoteAllocas(Function func) {
        Set<AllocaInst> allocas = new HashSet<>();
        for (BasicBlock block : func)
            for (Instruction inst : block)
                if (inst instanceof AllocaInst allocaInst && isAllocaPromotable(allocaInst))
                    allocas.add(allocaInst);
        return allocas;
    }

    private void insertPhi(Function func, Map<BasicBlock, Set<BasicBlock>> df, Set<AllocaInst> allocas) {
        Set<AllocaInst> globals = new HashSet<>();
        Map<AllocaInst, Set<BasicBlock>> blocks = new HashMap<>();
        for (BasicBlock block : func) {
            Set<AllocaInst> varKill = new HashSet<>();
            for (Instruction inst : block) {
                switch (inst) {
                    case LoadInst loadInst -> {
                        if (loadInst.getOperand(0) instanceof AllocaInst allocaInst && allocas.contains(allocaInst) && !varKill.contains(allocaInst)) {
                            globals.add(allocaInst);
                        }
                    }
                    case StoreInst storeInst -> {
                        if (storeInst.getOperand(1) instanceof AllocaInst allocaInst && allocas.contains(allocaInst)) {
                            varKill.add(allocaInst);
                            blocks.computeIfAbsent(allocaInst, k -> new HashSet<>()).add(block);
                        }
                    }
                    default -> {
                    }
                }
            }
        }
        for (AllocaInst global : globals) {
            Queue<BasicBlock> workList = new ArrayDeque<>(blocks.get(global));
            while (!workList.isEmpty()) {
                BasicBlock block = workList.poll();
                for (BasicBlock d : df.get(block)) {
                    if (!globalPhiMap.get(d).containsKey(global)) {
                        globalPhiMap.get(d).put(global, new PHINode(d, global.getType().baseType()));
                        workList.offer(d);
                    }
                }
            }
        }
    }

    private void rename(BasicBlock block, Map<AllocaInst, Stack<Value>> replaceMap, Set<AllocaInst> allocaInsts, Map<BasicBlock, Set<BasicBlock>> domTree) {
        Map<AllocaInst, Integer> counter = new HashMap<>();
        for (Map.Entry<AllocaInst, PHINode> entry : globalPhiMap.get(block).entrySet()) {
            AllocaInst allocaInst = entry.getKey();
            replaceMap.computeIfAbsent(allocaInst, k -> new Stack<>()).push(entry.getValue());
            counter.put(allocaInst, counter.getOrDefault(allocaInst, 0) + 1);
        }
        for (int i = 0; i < block.size(); i++) {
            switch (block.get(i)) {
                case StoreInst storeInst -> {
                    if (storeInst.getOperand(1) instanceof AllocaInst allocaInst && allocaInsts.contains(allocaInst)) {
                        block.remove(i);
                        i--;
                        replaceMap.computeIfAbsent(allocaInst, k -> new Stack<>()).push(storeInst.getOperand(0));
                        counter.put(allocaInst, counter.getOrDefault(allocaInst, 0) + 1);
                    }
                }
                case LoadInst loadInst -> {
                    if (loadInst.getOperand(0) instanceof AllocaInst allocaInst && allocaInsts.contains(allocaInst)) {
                        block.remove(i);
                        i--;
                        if (replaceMap.containsKey(allocaInst)) {
                            loadInst.replaceAllUseAs(replaceMap.get(allocaInst).peek());
                        } else {
                            loadInst.replaceAllUseAs(allocaInst.getType().baseType() == BasicType.I32 ? new ConstantNumber(0) : new ConstantNumber(0.0f));
                        }
                    }
                }
                default -> {
                }
            }
        }
        List<BasicBlock> nextBlocks = new ArrayList<>();
        if (block.getLast() instanceof BranchInst branchInst) {
            if (branchInst.isConditional()) {
                nextBlocks.add(branchInst.getOperand(1));
                nextBlocks.add(branchInst.getOperand(2));
            } else {
                nextBlocks.add(branchInst.getOperand(0));
            }
        }
        for (BasicBlock nextBlock : nextBlocks) {
            Map<AllocaInst, PHINode> phiMap = globalPhiMap.get(nextBlock);
            for (Map.Entry<AllocaInst, PHINode> phiEntry : phiMap.entrySet()) {
                AllocaInst allocaInst = phiEntry.getKey();
                PHINode phiNode = phiMap.get(allocaInst);
                if (replaceMap.containsKey(allocaInst)) {
                    phiNode.add(block, new Use(phiNode, replaceMap.get(allocaInst).peek()));
                } else {
                    phiNode.add(block, new Use(phiNode, allocaInst.getType().baseType() == BasicType.I32 ? new ConstantNumber(0) : new ConstantNumber(0.0f)));
                }
            }
        }
        if (domTree.containsKey(block)) {
            for (BasicBlock nextBlock : domTree.get(block)) {
                rename(nextBlock, replaceMap, allocaInsts, domTree);
            }
        }
        for (Map.Entry<AllocaInst, Integer> entry : counter.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                replaceMap.get(entry.getKey()).pop();
            }
            if (replaceMap.get(entry.getKey()).isEmpty()) {
                replaceMap.remove(entry.getKey());
            }
        }
    }

    private boolean isAllocaPromotable(AllocaInst allocaInst) {
        return allocaInst.getType() instanceof PointerType pointerType && pointerType.baseType() instanceof BasicType;
    }

    private void prunePhi(Function func) {
        boolean toContinue;
        do {
            toContinue = false;
            Queue<PHINode> workList = new ArrayDeque<>();
            Set<PHINode> marked = new HashSet<>();
            for (BasicBlock block : func) {
                toContinue |= globalPhiMap.get(block).entrySet().removeIf(entry -> entry.getValue().getUses().isEmpty());
            }
            for (int i = func.size() - 1; i >= 0; i--) {
                BasicBlock block = func.get(i);
                for (PHINode phiNode : globalPhiMap.get(block).values()) {
                    workList.offer(phiNode);
                    marked.add(phiNode);
                }
            }
            while (!workList.isEmpty()) {
                PHINode phiNode = workList.poll();
                marked.remove(phiNode);
                boolean flag = false;
                Value replaceValue = null;
                if (phiNode.size() == 2) {
                    Value value1 = phiNode.getOperand(0);
                    Value value2 = phiNode.getOperand(1);
                    if (value1 == phiNode) {
                        flag = true;
                        replaceValue = value2;
                    } else if (value2 == phiNode) {
                        flag = true;
                        replaceValue = value1;
                    }
                }
                if (flag) {
                    toContinue = true;
                    for (Use use : replaceValue.getUses()) {
                        use.setValue(replaceValue);
                        if (replaceValue instanceof PHINode replacePhiNode && !marked.contains(replacePhiNode)) {
                            workList.offer(replacePhiNode);
                            marked.add(replacePhiNode);
                        }
                    }
                }
            }
        } while (toContinue);
    }

    @Override
    public boolean runOnFunction(Function func) {
        if (func.isDeclare()) {
            return false;
        }
        Set<AllocaInst> promoteAllocas = analyzePromoteAllocas(func);
        if (promoteAllocas.isEmpty()) {
            return false;
        }
        globalPhiMap.clear();
        for (BasicBlock block : func)
            globalPhiMap.put(block, new HashMap<>());
        DomTreeAnalysis domTreeAnalysis = new DomTreeAnalysis(func);
        insertPhi(func, domTreeAnalysis.getDF(), promoteAllocas);
        rename(func.getFirst(), new HashMap<>(), promoteAllocas, domTreeAnalysis.getDomTree());
        prunePhi(func);
        for (Map.Entry<BasicBlock, Map<AllocaInst, PHINode>> phiEntry : globalPhiMap.entrySet()) {
            for (PHINode phiNode : phiEntry.getValue().values()) {
                BasicBlock block = phiEntry.getKey();
                block.add(0, phiNode);
            }
        }
        for (BasicBlock block : func) {
            for (int i = 0; i < block.size(); i++) {
                Instruction inst = block.get(i);
                if (inst instanceof AllocaInst allocaInst && isAllocaPromotable(allocaInst)) {
                    block.remove(i);
                    i--;
                }
            }
        }
        return true;
    }
}
