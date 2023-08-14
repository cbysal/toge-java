package compile.codegen.virgen.pass;

import common.Pair;
import compile.codegen.virgen.Block;
import compile.codegen.virgen.VReg;
import compile.codegen.virgen.VirtualFunction;
import compile.codegen.virgen.vir.*;
import compile.symbol.*;

import java.util.*;

public class FunctionInline extends Pass {
    public FunctionInline(Set<GlobalSymbol> globals, Map<String, VirtualFunction> funcs) {
        super(globals, funcs);
    }

    @Override
    public boolean run() {
        boolean modified = false;
        VirtualFunction toInlineFunc;
        do {
            toInlineFunc = null;
            Map<FuncSymbol, Set<FuncSymbol>> funcCallRelations = new HashMap<>();
            FuncSymbol mainFunc = null;
            for (VirtualFunction func : funcs.values())
                funcCallRelations.put(func.getSymbol(), new HashSet<>());
            for (VirtualFunction func : funcs.values()) {
                if (func.getSymbol().getName().equals("main"))
                    mainFunc = func.getSymbol();
                Set<FuncSymbol> calledFuncs = funcCallRelations.get(func.getSymbol());
                for (Block block : func.getBlocks())
                    for (VIR ir : block)
                        if (ir instanceof CallVIR callVIR && funcCallRelations.containsKey(callVIR.func()))
                            calledFuncs.add(callVIR.func());
            }
            if (mainFunc == null)
                throw new RuntimeException("No main function!");
            removeUselessFunctions(mainFunc, funcCallRelations);
            for (VirtualFunction func : funcs.values())
                if (!func.getSymbol().getName().equals("main") && funcCallRelations.get(func.getSymbol()).isEmpty())
                    toInlineFunc = func;
            if (toInlineFunc != null) {
                functionInlineForSingle(toInlineFunc);
                modified = true;
            }
        } while (toInlineFunc != null);
        return modified;
    }

    private void functionInlineForSingle(VirtualFunction toInlineFunc) {
        FuncSymbol toInlineSymbol = toInlineFunc.getSymbol();
        for (VirtualFunction func : funcs.values()) {
            List<Block> blocks = func.getBlocks();
            Map<VReg, Pair<DataSymbol, List<VIRItem>>> arrayParamMap = new HashMap<>();
            for (Block oldBlock : blocks)
                for (VIR ir : oldBlock)
                    if (ir instanceof LoadVIR loadVIR && loadVIR.indexes().size() != loadVIR.symbol().getDimensionSize())
                        arrayParamMap.put(loadVIR.target(), new Pair<>(loadVIR.symbol(), loadVIR.indexes()));
            for (int blockId = 0; blockId < blocks.size(); blockId++) {
                Block curBlock = blocks.get(blockId);
                for (int irId = 0; irId < curBlock.size(); irId++) {
                    VIR ir = curBlock.get(irId);
                    if (ir instanceof CallVIR toReplaceCall && toReplaceCall.func().equals(toInlineSymbol)) {
                        Map<ParamSymbol, VReg> paramToRegMap = new HashMap<>();
                        Map<VReg, VReg> paramPropagationMap = new HashMap<>();
                        Map<LocalSymbol, LocalSymbol> oldToNewLocalMap = new HashMap<>();
                        Block preCallBlock = new Block();
                        Block lastBlock = new Block();
                        for (int i = 0; i < toReplaceCall.params().size(); i++) {
                            ParamSymbol param = toReplaceCall.func().getParams().get(i);
                            if (toReplaceCall.params().get(i) instanceof VReg reg) {
                                VReg newReg = new VReg(reg.getType(), reg.getSize());
                                paramToRegMap.put(param, newReg);
                                preCallBlock.add(new MovVIR(newReg, reg));
                                paramPropagationMap.put(newReg, reg);
                                continue;
                            }
                            if (toReplaceCall.params().get(i) instanceof Value value) {
                                VReg reg = new VReg(toReplaceCall.func().getParams().get(i).getType(), 4);
                                paramToRegMap.put(param, reg);
                                if (value.getType() == Type.FLOAT)
                                    preCallBlock.add(new LiVIR(reg, value.getFloat()));
                                else
                                    preCallBlock.add(new LiVIR(reg, value.getInt()));
                                continue;
                            }
                            throw new RuntimeException();
                        }
                        for (LocalSymbol local : toInlineFunc.getLocals()) {
                            LocalSymbol newLocal = local.clone();
                            oldToNewLocalMap.put(local, newLocal);
                            func.addLocal(newLocal);
                        }
                        Map<VReg, VReg> oldToNewRegMap = new HashMap<>();
                        List<Block> oldBlocks = toInlineFunc.getBlocks();
                        List<Block> newBlocks = new ArrayList<>();
                        Map<Block, Block> oldToNewBlockMap = new HashMap<>();
                        for (Block oldBlock : oldBlocks) {
                            Block newBlock = new Block();
                            newBlocks.add(newBlock);
                            oldToNewBlockMap.put(oldBlock, newBlock);
                        }
                        for (Block oldBlock : oldBlocks) {
                            Block newBlock = oldToNewBlockMap.get(oldBlock);
                            if (oldBlock.getDefaultBlock() == null)
                                newBlock.setDefaultBlock(lastBlock);
                            else
                                newBlock.setDefaultBlock(oldToNewBlockMap.get(oldBlock.getDefaultBlock()));
                            for (Pair<Block.Cond, Block> oldCondBlockEntry : oldBlock.getCondBlocks()) {
                                Block.Cond oldCond = oldCondBlockEntry.first();
                                Block oldCondBlock = oldCondBlockEntry.second();
                                VIRItem left = oldCond.left();
                                if (left instanceof VReg reg) {
                                    if (!oldToNewRegMap.containsKey(reg))
                                        oldToNewRegMap.put(reg, new VReg(reg.getType(), reg.getSize()));
                                    left = oldToNewRegMap.get(reg);
                                }
                                VIRItem right = oldCond.right();
                                if (right instanceof VReg reg) {
                                    if (!oldToNewRegMap.containsKey(reg))
                                        oldToNewRegMap.put(reg, new VReg(reg.getType(), reg.getSize()));
                                    right = oldToNewRegMap.get(reg);
                                }
                                Block.Cond newCond = new Block.Cond(oldCond.type(), left, right);
                                Block newCondBlock;
                                if (oldCondBlock == null)
                                    newCondBlock = lastBlock;
                                else
                                    newCondBlock = oldToNewBlockMap.get(oldCondBlock);
                                newBlock.setCondBlock(newCond, newCondBlock);
                            }
                        }
                        for (Block oldBlock : oldBlocks) {
                            Block newBlock = oldToNewBlockMap.get(oldBlock);
                            Map<VReg, Map<VReg, Block>> oldPhiMap = oldBlock.getPhiMap();
                            Map<VReg, Map<VReg, Block>> newPhiMap = newBlock.getPhiMap();
                            for (Map.Entry<VReg, Map<VReg, Block>> entry : oldPhiMap.entrySet()) {
                                VReg target = entry.getKey();
                                if (!oldToNewRegMap.containsKey(target))
                                    oldToNewRegMap.put(target, new VReg(target.getType(), target.getSize()));
                                target = oldToNewRegMap.get(target);
                                newPhiMap.put(target, new HashMap<>());
                                for (Map.Entry<VReg, Block> regWithBlock : entry.getValue().entrySet()) {
                                    VReg source = regWithBlock.getKey();
                                    if (!oldToNewRegMap.containsKey(source))
                                        oldToNewRegMap.put(source, new VReg(source.getType(), source.getSize()));
                                    source = oldToNewRegMap.get(source);
                                    newPhiMap.get(target).put(source, oldToNewBlockMap.get(regWithBlock.getValue()));
                                }
                            }
                            for (VIR toReplaceIR : oldBlock) {
                                if (toReplaceIR instanceof BinaryVIR binaryVIR) {
                                    VReg target = binaryVIR.target();
                                    if (!oldToNewRegMap.containsKey(target))
                                        oldToNewRegMap.put(target, new VReg(target.getType(), target.getSize()));
                                    target = oldToNewRegMap.get(target);
                                    VIRItem left = binaryVIR.left();
                                    if (left instanceof VReg reg) {
                                        if (!oldToNewRegMap.containsKey(reg))
                                            oldToNewRegMap.put(reg, new VReg(reg.getType(), reg.getSize()));
                                        left = oldToNewRegMap.get(reg);
                                    }
                                    VIRItem right = binaryVIR.right();
                                    if (right instanceof VReg reg) {
                                        if (!oldToNewRegMap.containsKey(reg))
                                            oldToNewRegMap.put(reg, new VReg(reg.getType(), reg.getSize()));
                                        right = oldToNewRegMap.get(reg);
                                    }
                                    newBlock.add(new BinaryVIR(binaryVIR.type(), target, left, right));
                                    continue;
                                }
                                if (toReplaceIR instanceof CallVIR callVIR) {
                                    List<VIRItem> params = new ArrayList<>();
                                    for (VIRItem param : callVIR.params()) {
                                        if (param instanceof VReg reg) {
                                            if (!oldToNewRegMap.containsKey(reg))
                                                oldToNewRegMap.put(reg, new VReg(reg.getType(), reg.getSize()));
                                            params.add(oldToNewRegMap.get(reg));
                                        } else
                                            params.add(param);
                                    }
                                    VReg retVal = callVIR.target();
                                    if (retVal != null) {
                                        if (!oldToNewRegMap.containsKey(retVal))
                                            oldToNewRegMap.put(retVal, new VReg(retVal.getType(), retVal.getSize()));
                                        retVal = oldToNewRegMap.get(retVal);
                                    }
                                    newBlock.add(new CallVIR(callVIR.func(), retVal, params));
                                    continue;
                                }
                                if (toReplaceIR instanceof LiVIR liVIR) {
                                    VReg target = liVIR.target();
                                    if (!oldToNewRegMap.containsKey(target))
                                        oldToNewRegMap.put(target, new VReg(target.getType(), target.getSize()));
                                    target = oldToNewRegMap.get(target);
                                    newBlock.add(new LiVIR(target, liVIR.value()));
                                    continue;
                                }
                                if (toReplaceIR instanceof LoadVIR loadVIR) {
                                    if (loadVIR.symbol() instanceof ParamSymbol paramSymbol) {
                                        VReg toReplaceReg = paramToRegMap.get(paramSymbol);
                                        VReg target = loadVIR.target();
                                        if (!oldToNewRegMap.containsKey(target))
                                            oldToNewRegMap.put(target, new VReg(target.getType(), target.getSize()));
                                        target = oldToNewRegMap.get(target);
                                        if (paramSymbol.isSingle()) {
                                            newBlock.add(new MovVIR(target, toReplaceReg));
                                        } else {
                                            Pair<DataSymbol, List<VIRItem>> toReplaceSymbol =
                                                    arrayParamMap.get(paramPropagationMap.get(toReplaceReg));
                                            List<VIRItem> indexes = new ArrayList<>(toReplaceSymbol.second());
                                            for (VIRItem index : loadVIR.indexes()) {
                                                if (index instanceof VReg reg) {
                                                    if (!oldToNewRegMap.containsKey(reg))
                                                        oldToNewRegMap.put(reg, new VReg(reg.getType(), reg.getSize()));
                                                    indexes.add(oldToNewRegMap.get(reg));
                                                } else
                                                    indexes.add(index);
                                            }
                                            newBlock.add(new LoadVIR(target, toReplaceSymbol.first(), indexes));
                                        }
                                        continue;
                                    }
                                    VReg target = loadVIR.target();
                                    if (!oldToNewRegMap.containsKey(target))
                                        oldToNewRegMap.put(target, new VReg(target.getType(), target.getSize()));
                                    target = oldToNewRegMap.get(target);
                                    List<VIRItem> indexes = new ArrayList<>();
                                    for (VIRItem index : loadVIR.indexes()) {
                                        if (index instanceof VReg reg) {
                                            if (!oldToNewRegMap.containsKey(reg))
                                                oldToNewRegMap.put(reg, new VReg(reg.getType(), reg.getSize()));
                                            indexes.add(oldToNewRegMap.get(reg));
                                        } else
                                            indexes.add(index);
                                    }
                                    DataSymbol symbol = loadVIR.symbol();
                                    if (symbol instanceof LocalSymbol local)
                                        symbol = oldToNewLocalMap.get(local);
                                    newBlock.add(new LoadVIR(target, symbol, indexes));
                                    continue;
                                }
                                if (toReplaceIR instanceof MovVIR movVIR) {
                                    VReg target = movVIR.target();
                                    if (!oldToNewRegMap.containsKey(target))
                                        oldToNewRegMap.put(target, new VReg(target.getType(), target.getSize()));
                                    target = oldToNewRegMap.get(target);
                                    VReg source = movVIR.source();
                                    if (!oldToNewRegMap.containsKey(source))
                                        oldToNewRegMap.put(source, new VReg(source.getType(), source.getSize()));
                                    source = oldToNewRegMap.get(source);
                                    newBlock.add(new MovVIR(target, source));
                                    continue;
                                }
                                if (toReplaceIR instanceof RetVIR retVIR) {
                                    if (retVIR.retVal() != null) {
                                        VReg retVal = retVIR.retVal();
                                        if (!oldToNewRegMap.containsKey(retVal))
                                            oldToNewRegMap.put(retVal, new VReg(retVal.getType(), retVal.getSize()));
                                        retVal = oldToNewRegMap.get(retVal);
                                        newBlock.add(new MovVIR(toReplaceCall.target(), retVal));
                                    }
                                    continue;
                                }
                                if (toReplaceIR instanceof StoreVIR storeVIR) {
                                    if (storeVIR.symbol() instanceof ParamSymbol paramSymbol) {
                                        VReg toReplaceReg = paramToRegMap.get(paramSymbol);
                                        VReg source = storeVIR.source();
                                        if (!oldToNewRegMap.containsKey(source))
                                            oldToNewRegMap.put(source, new VReg(source.getType(), source.getSize()));
                                        source = oldToNewRegMap.get(source);
                                        if (paramSymbol.isSingle()) {
                                            newBlock.add(new MovVIR(toReplaceReg, source));
                                        } else {
                                            Pair<DataSymbol, List<VIRItem>> toReplaceSymbol =
                                                    arrayParamMap.get(paramPropagationMap.get(toReplaceReg));
                                            List<VIRItem> indexes = new ArrayList<>(toReplaceSymbol.second());
                                            for (VIRItem index : storeVIR.indexes()) {
                                                if (index instanceof VReg reg) {
                                                    if (!oldToNewRegMap.containsKey(reg))
                                                        oldToNewRegMap.put(reg, new VReg(reg.getType(), reg.getSize()));
                                                    indexes.add(oldToNewRegMap.get(reg));
                                                } else
                                                    indexes.add(index);
                                            }
                                            newBlock.add(new StoreVIR(toReplaceSymbol.first(), indexes, source));
                                        }
                                        continue;
                                    }
                                    DataSymbol symbol = storeVIR.symbol();
                                    if (symbol instanceof LocalSymbol local)
                                        symbol = oldToNewLocalMap.get(local);
                                    List<VIRItem> indexes = new ArrayList<>();
                                    for (VIRItem index : storeVIR.indexes()) {
                                        if (index instanceof VReg reg) {
                                            if (!oldToNewRegMap.containsKey(reg))
                                                oldToNewRegMap.put(reg, new VReg(reg.getType(), reg.getSize()));
                                            indexes.add(oldToNewRegMap.get(reg));
                                        } else
                                            indexes.add(index);
                                    }
                                    VReg source = storeVIR.source();
                                    if (!oldToNewRegMap.containsKey(source))
                                        oldToNewRegMap.put(source, new VReg(source.getType(), source.getSize()));
                                    source = oldToNewRegMap.get(source);
                                    newBlock.add(new StoreVIR(symbol, indexes, source));
                                    continue;
                                }
                                if (toReplaceIR instanceof UnaryVIR unaryVIR) {
                                    VReg target = unaryVIR.target();
                                    if (!oldToNewRegMap.containsKey(target))
                                        oldToNewRegMap.put(target, new VReg(target.getType(), target.getSize()));
                                    target = oldToNewRegMap.get(target);
                                    VIRItem source = unaryVIR.source();
                                    if (source instanceof VReg reg) {
                                        if (!oldToNewRegMap.containsKey(reg))
                                            oldToNewRegMap.put(reg, new VReg(reg.getType(), reg.getSize()));
                                        source = oldToNewRegMap.get(reg);
                                    }
                                    newBlock.add(new UnaryVIR(unaryVIR.type(), target, source));
                                    continue;
                                }
                                throw new RuntimeException();
                            }
                        }
                        lastBlock.setDefaultBlock(curBlock.getDefaultBlock());
                        curBlock.getCondBlocks().forEach(lastBlock::setCondBlock);
                        curBlock.setDefaultBlock(preCallBlock);
                        curBlock.clearCondBlocks();
                        preCallBlock.setDefaultBlock(newBlocks.get(0));
                        for (int i = irId + 1; i < curBlock.size(); i++) {
                            lastBlock.add(curBlock.get(i));
                        }
                        while (curBlock.size() > irId) {
                            curBlock.remove(curBlock.size() - 1);
                        }
                        func.addBlock(blockId + 1, preCallBlock);
                        func.addBlocks(blockId + 2, newBlocks);
                        func.addBlock(blockId + newBlocks.size() + 2, lastBlock);
                        reassignPhiBlock(func);
                        removePhiConflict(func);
                        break;
                    }
                }
            }
        }
    }

    private void reassignPhiBlock(VirtualFunction func) {
        Map<VReg, Block> regToBlockMap = new HashMap<>();
        for (Block block : func.getBlocks()) {
            for (VReg reg : block.getPhiMap().keySet())
                regToBlockMap.put(reg, block);
            for (VIR ir : block)
                if (ir.getWrite() != null)
                    regToBlockMap.put(ir.getWrite(), block);
        }
        for (Block block : func.getBlocks())
            for (Map<VReg, Block> regsWithBlock : block.getPhiMap().values())
                for (Map.Entry<VReg, Block> entry : regsWithBlock.entrySet())
                    entry.setValue(regToBlockMap.get(entry.getKey()));
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

    private void removeUselessFunctions(FuncSymbol mainFunc, Map<FuncSymbol, Set<FuncSymbol>> funcCallRelations) {
        Set<FuncSymbol> usedFuncs = new HashSet<>();
        Queue<FuncSymbol> frontier = new LinkedList<>();
        frontier.offer(mainFunc);
        while (!frontier.isEmpty()) {
            FuncSymbol func = frontier.poll();
            if (usedFuncs.contains(func))
                continue;
            usedFuncs.add(func);
            for (FuncSymbol calledFunc : funcCallRelations.getOrDefault(func, Set.of()))
                frontier.offer(calledFunc);
        }
        List<VirtualFunction> toRemoveFuncs = new ArrayList<>();
        for (VirtualFunction func : funcs.values())
            if (!usedFuncs.contains(func.getSymbol()))
                toRemoveFuncs.add(func);
        for (VirtualFunction func : toRemoveFuncs)
            funcs.remove(func.getSymbol().getName());
    }
}
