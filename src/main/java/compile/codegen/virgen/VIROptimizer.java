package compile.codegen.virgen;

import common.Pair;
import compile.codegen.virgen.vir.*;
import compile.symbol.*;

import java.util.*;
import java.util.stream.Collectors;

public class VIROptimizer {
    private boolean isProcessed = false;
    private final Map<String, GlobalSymbol> consts;
    private final Map<String, GlobalSymbol> globals;
    private final Map<String, VirtualFunction> funcs;

    public VIROptimizer(Map<String, GlobalSymbol> consts, Map<String, GlobalSymbol> globals, Map<String,
            VirtualFunction> funcs) {
        this.consts = consts;
        this.globals = globals;
        this.funcs = funcs;
    }

    private void checkIfIsProcessed() {
        if (isProcessed)
            return;
        isProcessed = true;
        singleLocal2Reg();
        removeUselessJump();
        removeEmptyBlocks();
        constPass();
        deadcodeElimination();
        functionInline();
        deadcodeElimination();
        removeUselessJump();
        removeEmptyBlocks();
    }

    private void constPass() {
        int sizeBefore, sizeAfter;
        do {
            sizeBefore = funcs.values().stream().mapToInt(VirtualFunction::countIRs).sum();
            for (VirtualFunction func : funcs.values()) {
                List<Block> oldBlocks = func.getBlocks();
                List<Block> newBlocks = new ArrayList<>();
                Map<Block, Block> oldToNewMap = new HashMap<>();
                for (Block oldBlock : oldBlocks) {
                    Block newBlock = new Block();
                    newBlocks.add(newBlock);
                    oldToNewMap.put(oldBlock, newBlock);
                }
                for (Block oldBlock : oldBlocks) {
                    Map<VReg, Integer> regToValueMap = new HashMap<>();
                    Block newBlock = oldToNewMap.get(oldBlock);
                    for (VIR ir : oldBlock) {
                        if (ir instanceof BinaryVIR binaryVIR) {
                            VIRItem left = binaryVIR.getLeft();
                            VIRItem right = binaryVIR.getRight();
                            if (left instanceof VReg reg && regToValueMap.containsKey(reg))
                                left = reg.getType() == Type.FLOAT ?
                                        new Value(Float.intBitsToFloat(regToValueMap.get(reg))) :
                                        new Value(regToValueMap.get(reg));
                            if (right instanceof VReg reg && regToValueMap.containsKey(reg))
                                right = reg.getType() == Type.FLOAT ?
                                        new Value(Float.intBitsToFloat(regToValueMap.get(reg))) :
                                        new Value(regToValueMap.get(reg));
                            newBlock.add(new BinaryVIR(binaryVIR.getType(), binaryVIR.getResult(), left, right));
                            continue;
                        }
                        if (ir instanceof JVIR jVIR) {
                            newBlock.add(new JVIR(oldToNewMap.get(jVIR.getBlock())));
                            continue;
                        }
                        if (ir instanceof BVIR bVIR) {
                            VIRItem left = bVIR.getLeft();
                            VIRItem right = bVIR.getRight();
                            if (left instanceof VReg reg && regToValueMap.containsKey(reg))
                                left = reg.getType() == Type.FLOAT ?
                                        new Value(Float.intBitsToFloat(regToValueMap.get(reg))) :
                                        new Value(regToValueMap.get(reg));
                            if (right instanceof VReg reg && regToValueMap.containsKey(reg))
                                right = reg.getType() == Type.FLOAT ?
                                        new Value(Float.intBitsToFloat(regToValueMap.get(reg))) :
                                        new Value(regToValueMap.get(reg));
                            newBlock.add(new BVIR(bVIR.getType(), left, right, oldToNewMap.get(bVIR.getTrueBlock()),
                                    oldToNewMap.get(bVIR.getFalseBlock())));
                            continue;
                        }
                        if (ir instanceof CallVIR callVIR) {
                            List<VIRItem> params = callVIR.getParams();
                            for (int j = 0; j < params.size(); j++)
                                if (params.get(j) instanceof VReg reg && regToValueMap.containsKey(reg))
                                    params.set(j, reg.getType() == Type.FLOAT ?
                                            new Value(Float.intBitsToFloat(regToValueMap.get(reg))) :
                                            new Value(regToValueMap.get(reg)));
                            newBlock.add(ir);
                            continue;
                        }
                        if (ir instanceof LIVIR liVIR) {
                            regToValueMap.put(liVIR.getTarget(), liVIR.getValue());
                            newBlock.add(ir);
                            continue;
                        }
                        if (ir instanceof LoadVIR loadVIR) {
                            List<VIRItem> dimensions = loadVIR.getDimensions();
                            for (int j = 0; j < dimensions.size(); j++)
                                if (dimensions.get(j) instanceof VReg reg && regToValueMap.containsKey(reg))
                                    dimensions.set(j, reg.getType() == Type.FLOAT ?
                                            new Value(Float.intBitsToFloat(regToValueMap.get(reg))) :
                                            new Value(regToValueMap.get(reg)));
                            newBlock.add(ir);
                            continue;
                        }
                        if (ir instanceof MovVIR movVIR) {
                            if (regToValueMap.containsKey(movVIR.getSource())) {
                                newBlock.add(new LIVIR(movVIR.getTarget(), regToValueMap.get(movVIR.getSource())));
                                continue;
                            }
                            newBlock.add(ir);
                            continue;
                        }
                        if (ir instanceof StoreVIR storeVIR) {
                            List<VIRItem> dimensions = storeVIR.getDimensions();
                            for (int j = 0; j < dimensions.size(); j++)
                                if (dimensions.get(j) instanceof VReg reg && regToValueMap.containsKey(reg))
                                    dimensions.set(j, reg.getType() == Type.FLOAT ?
                                            new Value(Float.intBitsToFloat(regToValueMap.get(reg))) :
                                            new Value(regToValueMap.get(reg)));
                            newBlock.add(ir);
                            continue;
                        }
                        if (ir instanceof UnaryVIR unaryVIR) {
                            if (unaryVIR.getSource() instanceof VReg reg && regToValueMap.containsKey(reg)) {
                                newBlock.add(new UnaryVIR(unaryVIR.getType(), unaryVIR.getResult(),
                                        reg.getType() == Type.FLOAT ?
                                                new Value(Float.intBitsToFloat(regToValueMap.get(reg))) :
                                                new Value(regToValueMap.get(reg))));
                                continue;
                            }
                            newBlock.add(ir);
                            continue;
                        }
                        throw new RuntimeException();
                    }
                }
                func.setBlocks(newBlocks);
            }
            standardize();
            sizeAfter = funcs.values().stream().mapToInt(VirtualFunction::countIRs).sum();
        } while (sizeBefore != sizeAfter);
    }

    private void deadcodeElimination() {
        deadcodeEliminationOnReachability();
        deadcodeEliminationOnRoot();
    }

    private void deadcodeEliminationOnReachability() {
        for (VirtualFunction func : funcs.values()) {
            List<Block> blocks = func.getBlocks();
            Map<Block, Integer> blockIdMap = new HashMap<>();
            for (int i = 0; i < blocks.size(); i++) {
                blockIdMap.put(blocks.get(i), i);
            }
            Queue<Block> frontier = new ArrayDeque<>();
            frontier.offer(blocks.get(0));
            Set<Block> reachableBlocks = new HashSet<>();
            while (!frontier.isEmpty()) {
                Block curBlock = frontier.poll();
                if (reachableBlocks.contains(curBlock))
                    continue;
                reachableBlocks.add(curBlock);
                boolean toTail = true;
                for (int irId = 0; irId < curBlock.size(); irId++) {
                    VIR ir = curBlock.get(irId);
                    if (ir instanceof BVIR bVIR) {
                        frontier.add(bVIR.getTrueBlock());
                        frontier.add(bVIR.getFalseBlock());
                        continue;
                    }
                    if (ir instanceof JVIR jVIR) {
                        frontier.add(jVIR.getBlock());
                        toTail = false;
                        while (curBlock.size() > irId + 1) {
                            curBlock.remove(curBlock.size() - 1);
                        }
                        break;
                    }
                }
                if (toTail) {
                    int nextId = blockIdMap.get(curBlock) + 1;
                    if (nextId < blocks.size()) {
                        frontier.offer(blocks.get(nextId));
                    }
                }
            }
            List<Block> newBlocks = blocks.stream().filter(reachableBlocks::contains).collect(Collectors.toList());
            func.setBlocks(newBlocks);
        }
    }

    private void deadcodeEliminationOnRoot() {
        Set<VReg> usedRegs = new HashSet<>();
        Set<Symbol> usedSymbols = new HashSet<>();
        Map<VReg, Set<VReg>> regToRegMap = new HashMap<>();
        Map<VReg, Set<Symbol>> regToSymbolMap = new HashMap<>();
        Map<Symbol, Set<VReg>> symbolToRegMap = new HashMap<>();
        for (VirtualFunction func : funcs.values()) {
            if (func.getRetVal() != null)
                usedRegs.add(func.getRetVal());
            usedSymbols.addAll(func.getSymbol().getParams());
            List<Block> blocks = func.getBlocks();
            for (Block block : blocks) {
                for (VIR ir : block) {
                    if (ir instanceof BinaryVIR binaryVIR) {
                        if (binaryVIR.getLeft() instanceof VReg reg) {
                            Set<VReg> regs = regToRegMap.getOrDefault(binaryVIR.getResult(), new HashSet<>());
                            regs.add(reg);
                            regToRegMap.put(binaryVIR.getResult(), regs);
                        }
                        if (binaryVIR.getRight() instanceof VReg reg) {
                            Set<VReg> regs = regToRegMap.getOrDefault(binaryVIR.getResult(), new HashSet<>());
                            regs.add(reg);
                            regToRegMap.put(binaryVIR.getResult(), regs);
                        }
                        continue;
                    }
                    if (ir instanceof BVIR bVIR) {
                        if (bVIR.getLeft() instanceof VReg reg)
                            usedRegs.add(reg);
                        if (bVIR.getRight() instanceof VReg reg)
                            usedRegs.add(reg);
                        continue;
                    }
                    if (ir instanceof CallVIR callVIR) {
                        for (VIRItem param : callVIR.getParams())
                            if (param instanceof VReg reg)
                                usedRegs.add(reg);
                        continue;
                    }
                    if (ir instanceof LoadVIR loadVIR) {
                        Set<Symbol> symbols = regToSymbolMap.getOrDefault(loadVIR.getTarget(), new HashSet<>());
                        symbols.add(loadVIR.getSymbol());
                        regToSymbolMap.put(loadVIR.getTarget(), symbols);
                        for (VIRItem dimension : loadVIR.getDimensions())
                            if (dimension instanceof VReg reg) {
                                Set<VReg> regs = regToRegMap.getOrDefault(loadVIR.getTarget(), new HashSet<>());
                                regs.add(reg);
                                regToRegMap.put(loadVIR.getTarget(), regs);
                            }
                        continue;
                    }
                    if (ir instanceof MovVIR movVIR) {
                        Set<VReg> regs = regToRegMap.getOrDefault(movVIR.getTarget(), new HashSet<>());
                        regs.add(movVIR.getSource());
                        regToRegMap.put(movVIR.getTarget(), regs);
                        continue;
                    }
                    if (ir instanceof StoreVIR storeVIR) {
                        Set<VReg> regs = symbolToRegMap.getOrDefault(storeVIR.getSymbol(), new HashSet<>());
                        regs.add(storeVIR.getSource());
                        for (VIRItem dimension : storeVIR.getDimensions())
                            if (dimension instanceof VReg reg)
                                regs.add(reg);
                        symbolToRegMap.put(storeVIR.getSymbol(), regs);
                        continue;
                    }
                    if (ir instanceof UnaryVIR unaryVIR) {
                        if (unaryVIR.getSource() instanceof VReg reg) {
                            Set<VReg> regs = regToRegMap.getOrDefault(unaryVIR.getResult(), new HashSet<>());
                            regs.add(reg);
                            regToRegMap.put(unaryVIR.getResult(), regs);
                        }
                    }
                }
            }
        }
        int sizeBefore, sizeAfter;
        do {
            sizeBefore = usedRegs.size() + usedSymbols.size();
            Set<VReg> newRegs = new HashSet<>();
            Set<Symbol> newSymbols = new HashSet<>();
            for (VReg reg : usedRegs) {
                newRegs.addAll(regToRegMap.getOrDefault(reg, Set.of()));
                newSymbols.addAll(regToSymbolMap.getOrDefault(reg, Set.of()));
            }
            for (Symbol symbol : usedSymbols)
                newRegs.addAll(symbolToRegMap.getOrDefault(symbol, Set.of()));
            usedRegs.addAll(newRegs);
            usedSymbols.addAll(newSymbols);
            sizeAfter = usedRegs.size() + usedSymbols.size();
        } while (sizeBefore != sizeAfter);
        for (VirtualFunction func : funcs.values()) {
            List<Block> oldBlocks = func.getBlocks();
            List<Block> newBlocks = new ArrayList<>();
            Map<Block, Block> oldToNewMap = new HashMap<>();
            for (Block oldBlock : oldBlocks) {
                Block newBlock = new Block();
                newBlocks.add(newBlock);
                oldToNewMap.put(oldBlock, newBlock);
            }
            for (Block oldBlock : oldBlocks) {
                Block newBlock = oldToNewMap.get(oldBlock);
                for (VIR ir : oldBlock) {
                    if (ir instanceof BinaryVIR binaryVIR) {
                        boolean flag = usedRegs.contains(binaryVIR.getResult());
                        if (flag && binaryVIR.getLeft() instanceof VReg reg && !usedRegs.contains(reg))
                            flag = false;
                        if (flag && binaryVIR.getRight() instanceof VReg reg && !usedRegs.contains(reg))
                            flag = false;
                        if (flag)
                            newBlock.add(ir);
                        continue;
                    }
                    if (ir instanceof JVIR jVIR) {
                        newBlock.add(new JVIR(oldToNewMap.get(jVIR.getBlock())));
                        continue;
                    }
                    if (ir instanceof BVIR bVIR) {
                        newBlock.add(new BVIR(bVIR.getType(), bVIR.getLeft(), bVIR.getRight(),
                                oldToNewMap.get(bVIR.getTrueBlock()), oldToNewMap.get(bVIR.getFalseBlock())));
                        continue;
                    }
                    if (ir instanceof LIVIR liVIR) {
                        if (usedRegs.contains(liVIR.getTarget()))
                            newBlock.add(ir);
                        continue;
                    }
                    if (ir instanceof LoadVIR loadVIR) {
                        boolean flag =
                                usedRegs.contains(loadVIR.getTarget()) && usedSymbols.contains(loadVIR.getSymbol());
                        for (VIRItem dimension : loadVIR.getDimensions())
                            if (flag && dimension instanceof VReg reg && !usedRegs.contains(reg))
                                flag = false;
                        if (flag)
                            newBlock.add(ir);
                        continue;
                    }
                    if (ir instanceof MovVIR movVIR) {
                        if (usedRegs.contains(movVIR.getTarget()) && usedRegs.contains(movVIR.getSource()))
                            newBlock.add(ir);
                        continue;
                    }
                    if (ir instanceof StoreVIR storeVIR) {
                        boolean flag =
                                usedRegs.contains(storeVIR.getSource()) && usedSymbols.contains(storeVIR.getSymbol());
                        for (VIRItem dimension : storeVIR.getDimensions())
                            if (dimension instanceof VReg reg && !usedRegs.contains(reg))
                                flag = false;
                        if (flag)
                            newBlock.add(ir);
                        continue;
                    }
                    if (ir instanceof UnaryVIR unaryVIR) {
                        boolean flag = usedRegs.contains(unaryVIR.getResult());
                        if (flag && unaryVIR.getSource() instanceof VReg reg && !usedRegs.contains(reg))
                            flag = false;
                        if (flag)
                            newBlock.add(ir);
                        continue;
                    }
                    newBlock.add(ir);
                }
            }
            func.setBlocks(newBlocks);
        }
    }

    private void functionInline() {
        VirtualFunction toInlineFunc;
        do {
            toInlineFunc = null;
            Map<FuncSymbol, Set<FuncSymbol>> funcCallRelations = new HashMap<>();
            FuncSymbol mainFunc = null;
            for (VirtualFunction func : funcs.values()) {
                if (func.getSymbol().getName().equals("main"))
                    mainFunc = func.getSymbol();
                Set<FuncSymbol> calledFuncs = new HashSet<>();
                for (Block block : func.getBlocks())
                    for (VIR ir : block)
                        if (ir instanceof CallVIR callVIR)
                            calledFuncs.add(callVIR.getFunc());
                funcCallRelations.put(func.getSymbol(), calledFuncs);
            }
            if (mainFunc == null)
                throw new RuntimeException("No main function!");
            removeUselessFunctions(mainFunc, funcCallRelations);
            for (VirtualFunction func : funcs.values())
                if (!func.getSymbol().getName().equals("main") && funcCallRelations.get(func.getSymbol()).isEmpty())
                    toInlineFunc = func;
            if (toInlineFunc != null)
                functionInlineForSingle(toInlineFunc);
        } while (toInlineFunc != null);
    }

    private void functionInlineForSingle(VirtualFunction toInlineFunc) {
        FuncSymbol toInlineSymbol = toInlineFunc.getSymbol();
        for (VirtualFunction func : funcs.values()) {
            List<Block> blocks = func.getBlocks();
            Map<VReg, Pair<DataSymbol, List<VIRItem>>> arrayParamMap = new HashMap<>();
            for (Block oldBlock : blocks)
                for (VIR ir : oldBlock)
                    if (ir instanceof LoadVIR loadVIR && loadVIR.getDimensions().size() != loadVIR.getSymbol().getDimensionSize())
                        arrayParamMap.put(loadVIR.getTarget(), new Pair<>(loadVIR.getSymbol(),
                                loadVIR.getDimensions()));
            for (int blockId = 0; blockId < blocks.size(); blockId++) {
                Block curBlock = blocks.get(blockId);
                for (int irId = 0; irId < curBlock.size(); irId++) {
                    VIR ir = curBlock.get(irId);
                    if (ir instanceof CallVIR toReplaceCall && toReplaceCall.getFunc().equals(toInlineSymbol)) {
                        Map<ParamSymbol, VReg> paramToRegMap = new HashMap<>();
                        Block preCallBlock = new Block();
                        for (int i = 0; i < toReplaceCall.getParams().size(); i++) {
                            ParamSymbol param = toReplaceCall.getFunc().getParams().get(i);
                            if (toReplaceCall.getParams().get(i) instanceof VReg reg) {
                                paramToRegMap.put(param, reg);
                                continue;
                            }
                            if (toReplaceCall.getParams().get(i) instanceof Value value) {
                                VReg reg = new VReg(toReplaceCall.getFunc().getParams().get(i).getType());
                                paramToRegMap.put(param, reg);
                                if (value.getType() == Type.FLOAT)
                                    preCallBlock.add(new LIVIR(reg, value.getFloat()));
                                else
                                    preCallBlock.add(new LIVIR(reg, value.getInt()));
                                continue;
                            }
                            throw new RuntimeException();
                        }
                        Map<VReg, VReg> regMap = new HashMap<>();
                        if (toReplaceCall.getRetVal() != null)
                            regMap.put(toInlineFunc.getRetVal(), toReplaceCall.getRetVal());
                        List<Block> oldBlocks = toInlineFunc.getBlocks();
                        List<Block> newBlocks = new ArrayList<>();
                        Map<Block, Block> oldToNewMap = new HashMap<>();
                        for (Block oldBlock : oldBlocks) {
                            Block newBlock = new Block();
                            newBlocks.add(newBlock);
                            oldToNewMap.put(oldBlock, newBlock);
                        }
                        for (Block oldBlock : oldBlocks) {
                            Block newBlock = oldToNewMap.get(oldBlock);
                            for (VIR toReplaceIR : oldBlock) {
                                if (toReplaceIR instanceof BinaryVIR binaryVIR) {
                                    VReg result = binaryVIR.getResult();
                                    if (regMap.containsKey(result))
                                        result = regMap.get(result);
                                    else {
                                        result = new VReg(result.getType());
                                        regMap.put(binaryVIR.getResult(), result);
                                    }
                                    VIRItem left = binaryVIR.getLeft();
                                    if (left instanceof VReg reg) {
                                        if (regMap.containsKey(reg))
                                            left = regMap.get(reg);
                                        else {
                                            VReg newReg = new VReg(reg.getType());
                                            left = newReg;
                                            regMap.put(reg, newReg);
                                        }
                                    }
                                    VIRItem right = binaryVIR.getRight();
                                    if (right instanceof VReg reg) {
                                        if (regMap.containsKey(reg))
                                            right = regMap.get(reg);
                                        else {
                                            VReg newReg = new VReg(reg.getType());
                                            right = newReg;
                                            regMap.put(reg, newReg);
                                        }
                                    }
                                    newBlock.add(new BinaryVIR(binaryVIR.getType(), result, left, right));
                                    continue;
                                }
                                if (toReplaceIR instanceof JVIR jVIR) {
                                    Block targetBlock = oldToNewMap.get(jVIR.getBlock());
                                    newBlock.add(new JVIR(targetBlock));
                                    continue;
                                }
                                if (toReplaceIR instanceof BVIR bVIR) {
                                    Block newTrueBlock = oldToNewMap.get(bVIR.getTrueBlock());
                                    Block newFalseBlock = oldToNewMap.get(bVIR.getFalseBlock());
                                    VIRItem left = bVIR.getLeft();
                                    if (left instanceof VReg reg) {
                                        if (regMap.containsKey(reg))
                                            left = regMap.get(reg);
                                        else {
                                            VReg newReg = new VReg(reg.getType());
                                            left = newReg;
                                            regMap.put(reg, newReg);
                                        }
                                    }
                                    VIRItem right = bVIR.getRight();
                                    if (right instanceof VReg reg) {
                                        if (regMap.containsKey(reg))
                                            right = regMap.get(reg);
                                        else {
                                            VReg newReg = new VReg(reg.getType());
                                            right = newReg;
                                            regMap.put(reg, newReg);
                                        }
                                    }
                                    newBlock.add(new BVIR(bVIR.getType(), left, right, newTrueBlock, newFalseBlock));
                                    continue;
                                }
                                if (toReplaceIR instanceof CallVIR callVIR) {
                                    List<VIRItem> params = new ArrayList<>();
                                    for (VIRItem param : callVIR.getParams()) {
                                        if (param instanceof VReg reg) {
                                            if (regMap.containsKey(reg))
                                                params.add(regMap.get(reg));
                                            else {
                                                VReg newReg = new VReg(reg.getType());
                                                params.add(newReg);
                                                regMap.put(reg, newReg);
                                            }
                                        } else
                                            params.add(param);
                                    }
                                    VReg retVal = callVIR.getRetVal();
                                    if (regMap.containsKey(retVal))
                                        retVal = regMap.get(retVal);
                                    else {
                                        retVal = new VReg(retVal.getType());
                                        regMap.put(callVIR.getRetVal(), retVal);
                                    }
                                    newBlock.add(new CallVIR(callVIR.getFunc(), retVal, params));
                                    continue;
                                }
                                if (toReplaceIR instanceof LIVIR liVIR) {
                                    VReg target = liVIR.getTarget();
                                    if (regMap.containsKey(target))
                                        target = regMap.get(target);
                                    else {
                                        target = new VReg(target.getType());
                                        regMap.put(liVIR.getTarget(), target);
                                    }
                                    newBlock.add(new LIVIR(target, liVIR.getValue()));
                                    continue;
                                }
                                if (toReplaceIR instanceof LoadVIR loadVIR) {
                                    if (loadVIR.getSymbol() instanceof ParamSymbol paramSymbol) {
                                        VReg toReplaceReg = paramToRegMap.get(paramSymbol);
                                        VReg target = loadVIR.getTarget();
                                        if (regMap.containsKey(target))
                                            target = regMap.get(target);
                                        else {
                                            target = new VReg(target.getType());
                                            regMap.put(loadVIR.getTarget(), target);
                                        }
                                        if (paramSymbol.isSingle()) {
                                            newBlock.add(new MovVIR(target, toReplaceReg));
                                        } else {
                                            Pair<DataSymbol, List<VIRItem>> toReplaceSymbol =
                                                    arrayParamMap.get(toReplaceReg);
                                            List<VIRItem> dimensions = new ArrayList<>(toReplaceSymbol.second());
                                            for (VIRItem dimension : loadVIR.getDimensions()) {
                                                if (dimension instanceof VReg reg) {
                                                    if (regMap.containsKey(reg))
                                                        dimensions.add(regMap.get(reg));
                                                    else {
                                                        VReg newReg = new VReg(reg.getType());
                                                        dimensions.add(newReg);
                                                        regMap.put(reg, newReg);
                                                    }
                                                } else
                                                    dimensions.add(dimension);
                                            }
                                            newBlock.add(new LoadVIR(target, dimensions, toReplaceSymbol.first()));
                                        }
                                        continue;
                                    }
                                    VReg target = loadVIR.getTarget();
                                    if (regMap.containsKey(target))
                                        target = regMap.get(target);
                                    else {
                                        target = new VReg(target.getType());
                                        regMap.put(loadVIR.getTarget(), target);
                                    }
                                    List<VIRItem> dimensions = new ArrayList<>();
                                    for (VIRItem dimension : loadVIR.getDimensions()) {
                                        if (dimension instanceof VReg reg) {
                                            if (regMap.containsKey(reg))
                                                dimensions.add(regMap.get(reg));
                                            else {
                                                VReg newReg = new VReg(reg.getType());
                                                dimensions.add(newReg);
                                                regMap.put(reg, newReg);
                                            }
                                        } else
                                            dimensions.add(dimension);
                                    }
                                    if (loadVIR.getSymbol() instanceof LocalSymbol localSymbol)
                                        func.addLocal(localSymbol);
                                    newBlock.add(new LoadVIR(target, dimensions, loadVIR.getSymbol()));
                                    continue;
                                }
                                if (toReplaceIR instanceof MovVIR movVIR) {
                                    VReg target = movVIR.getTarget();
                                    if (regMap.containsKey(target))
                                        target = regMap.get(target);
                                    else {
                                        target = new VReg(target.getType());
                                        regMap.put(movVIR.getTarget(), target);
                                    }
                                    VReg source = movVIR.getSource();
                                    if (regMap.containsKey(source))
                                        source = regMap.get(source);
                                    else {
                                        source = new VReg(source.getType());
                                        regMap.put(movVIR.getSource(), source);
                                    }
                                    newBlock.add(new MovVIR(target, source));
                                    continue;
                                }
                                if (toReplaceIR instanceof StoreVIR storeVIR) {
                                    if (storeVIR.getSymbol() instanceof ParamSymbol paramSymbol) {
                                        VReg toReplaceReg = paramToRegMap.get(paramSymbol);
                                        VReg source = storeVIR.getSource();
                                        if (regMap.containsKey(source))
                                            source = regMap.get(source);
                                        else {
                                            source = new VReg(source.getType());
                                            regMap.put(storeVIR.getSource(), source);
                                        }
                                        if (paramSymbol.isSingle()) {
                                            newBlock.add(new MovVIR(toReplaceReg, source));
                                        } else {
                                            Pair<DataSymbol, List<VIRItem>> toReplaceSymbol =
                                                    arrayParamMap.get(toReplaceReg);
                                            List<VIRItem> dimensions = new ArrayList<>(toReplaceSymbol.second());
                                            for (VIRItem dimension : storeVIR.getDimensions()) {
                                                if (dimension instanceof VReg reg) {
                                                    if (regMap.containsKey(reg))
                                                        dimensions.add(regMap.get(reg));
                                                    else {
                                                        VReg newReg = new VReg(reg.getType());
                                                        dimensions.add(newReg);
                                                        regMap.put(reg, newReg);
                                                    }
                                                } else
                                                    dimensions.add(dimension);
                                            }
                                            newBlock.add(new StoreVIR(toReplaceSymbol.first(), dimensions, source));
                                        }
                                        continue;
                                    }
                                    if (storeVIR.getSymbol() instanceof LocalSymbol localSymbol)
                                        func.addLocal(localSymbol);
                                    List<VIRItem> dimensions = new ArrayList<>();
                                    for (VIRItem dimension : storeVIR.getDimensions()) {
                                        if (dimension instanceof VReg reg) {
                                            if (regMap.containsKey(reg))
                                                dimensions.add(regMap.get(reg));
                                            else {
                                                VReg newReg = new VReg(reg.getType());
                                                dimensions.add(newReg);
                                                regMap.put(reg, newReg);
                                            }
                                        } else
                                            dimensions.add(dimension);
                                    }
                                    VReg source = storeVIR.getSource();
                                    if (regMap.containsKey(source))
                                        source = regMap.get(source);
                                    else {
                                        source = new VReg(source.getType());
                                        regMap.put(storeVIR.getSource(), source);
                                    }
                                    newBlock.add(new StoreVIR(storeVIR.getSymbol(), dimensions, source));
                                    continue;
                                }
                                if (toReplaceIR instanceof UnaryVIR unaryVIR) {
                                    VReg result = unaryVIR.getResult();
                                    if (regMap.containsKey(result))
                                        result = regMap.get(result);
                                    else {
                                        result = new VReg(result.getType());
                                        regMap.put(unaryVIR.getResult(), result);
                                    }
                                    VIRItem source = unaryVIR.getSource();
                                    if (source instanceof VReg reg) {
                                        if (regMap.containsKey(reg))
                                            source = regMap.get(reg);
                                        else {
                                            VReg newReg = new VReg(reg.getType());
                                            source = newReg;
                                            regMap.put(reg, newReg);
                                        }
                                    }
                                    newBlock.add(new UnaryVIR(unaryVIR.getType(), result, source));
                                    continue;
                                }
                                throw new RuntimeException();
                            }
                        }
                        Block lastBlock = new Block();
                        for (int i = irId + 1; i < curBlock.size(); i++) {
                            lastBlock.add(curBlock.get(i));
                        }
                        while (curBlock.size() > irId) {
                            curBlock.remove(curBlock.size() - 1);
                        }
                        func.addBlock(blockId + 1, preCallBlock);
                        func.addBlocks(blockId + 2, newBlocks);
                        func.addBlock(blockId + newBlocks.size() + 2, lastBlock);
                        break;
                    }
                }
            }
        }
    }

    public void optimize() {
        checkIfIsProcessed();
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

    private void removeUselessJump() {
        for (VirtualFunction func : funcs.values()) {
            List<Block> blocks = func.getBlocks();
            for (int blockId = 0; blockId + 1 < blocks.size(); blockId++) {
                Block curBlock = blocks.get(blockId);
                Block nextBlock = blocks.get(blockId + 1);
                if (!curBlock.isEmpty()) {
                    VIR lastIR = curBlock.get(curBlock.size() - 1);
                    if (lastIR instanceof BVIR bVIR && bVIR.getTrueBlock() == nextBlock && bVIR.getFalseBlock() == nextBlock)
                        curBlock.remove(curBlock.size() - 1);
                }
            }
        }
    }

    private void removeEmptyBlocks() {
        for (VirtualFunction func : funcs.values()) {
            boolean toContinue;
            do {
                toContinue = false;
                List<Block> blocks = func.getBlocks();
                Block curBlock = null, nextBlock = null;
                for (int blockId = 0; blockId + 1 < blocks.size(); blockId++) {
                    curBlock = blocks.get(blockId);
                    nextBlock = blocks.get(blockId + 1);
                    if (curBlock.isEmpty()) {
                        blocks.remove(blockId);
                        toContinue = true;
                        break;
                    }
                }
                if (toContinue) {
                    for (Block block : blocks) {
                        for (int i = 0; i < block.size(); i++) {
                            if (block.get(i) instanceof BVIR bVIR && bVIR.getTrueBlock() == curBlock) {
                                block.set(i, new BVIR(bVIR.getType(), bVIR.getLeft(), bVIR.getRight(), nextBlock,
                                        bVIR.getFalseBlock()));
                                continue;
                            }
                            if (block.get(i) instanceof BVIR bVIR && bVIR.getFalseBlock() == curBlock) {
                                block.set(i, new BVIR(bVIR.getType(), bVIR.getLeft(), bVIR.getRight(),
                                        bVIR.getTrueBlock(), nextBlock));
                                continue;
                            }
                            if (block.get(i) instanceof JVIR jVIR && jVIR.getBlock() == curBlock) {
                                block.set(i, new JVIR(nextBlock));
                            }
                        }
                    }
                }
            } while (toContinue);
        }
    }

    private void singleLocal2Reg() {
        for (VirtualFunction func : funcs.values()) {
            Set<LocalSymbol> locals =
                    func.getLocals().stream().filter(DataSymbol::isSingle).collect(Collectors.toSet());
            Map<LocalSymbol, VReg> local2Reg = new HashMap<>();
            for (Block block : func.getBlocks()) {
                for (int i = 0; i < block.size(); i++) {
                    VIR ir = block.get(i);
                    if (ir instanceof LoadVIR loadVIR && loadVIR.getSymbol() instanceof LocalSymbol local && locals.contains(local)) {
                        VReg reg;
                        if (local2Reg.containsKey(local))
                            reg = local2Reg.get(local);
                        else {
                            reg = new VReg(local.getType());
                            local2Reg.put(local, reg);
                        }
                        MovVIR newIR = new MovVIR(loadVIR.getTarget(), reg);
                        block.set(i, newIR);
                        continue;
                    }
                    if (ir instanceof StoreVIR storeVIR && storeVIR.getSymbol() instanceof LocalSymbol local && locals.contains(local)) {
                        VReg reg;
                        if (local2Reg.containsKey(local))
                            reg = local2Reg.get(local);
                        else {
                            reg = new VReg(local.getType());
                            local2Reg.put(local, reg);
                        }
                        MovVIR newIR = new MovVIR(reg, storeVIR.getSource());
                        block.set(i, newIR);
                    }
                }
            }
        }
    }

    private void standardize() {
        for (VirtualFunction func : funcs.values()) {
            List<Block> oldBlocks = func.getBlocks();
            List<Block> newBlocks = new ArrayList<>();
            Map<Block, Block> oldToNewMap = new HashMap<>();
            for (Block oldBlock : oldBlocks) {
                Block newBlock = new Block();
                newBlocks.add(newBlock);
                oldToNewMap.put(oldBlock, newBlock);
            }
            for (Block oldBlock : oldBlocks) {
                Block newBlock = oldToNewMap.get(oldBlock);
                for (VIR ir : oldBlock) {
                    if (ir instanceof BinaryVIR binaryVIR && binaryVIR.getLeft() instanceof Value value1 && binaryVIR.getRight() instanceof Value value2) {
                        if (binaryVIR.getResult().getType() == Type.FLOAT)
                            newBlock.add(new LIVIR(binaryVIR.getResult(), switch (binaryVIR.getType()) {
                                case ADD -> value1.getFloat() + value2.getFloat();
                                case DIV -> value1.getFloat() / value2.getFloat();
                                case MUL -> value1.getFloat() * value2.getFloat();
                                case SUB -> value1.getFloat() - value2.getFloat();
                                default -> throw new RuntimeException();
                            }));
                        else
                            newBlock.add(new LIVIR(binaryVIR.getResult(), switch (binaryVIR.getType()) {
                                case ADD -> value1.getInt() + value2.getInt();
                                case DIV -> value1.getInt() / value2.getInt();
                                case EQ -> value1.eq(value2) ? 1 : 0;
                                case GE -> value1.ge(value2) ? 1 : 0;
                                case GT -> value1.gt(value2) ? 1 : 0;
                                case LE -> value1.le(value2) ? 1 : 0;
                                case LT -> value1.lt(value2) ? 1 : 0;
                                case MOD -> value1.getInt() % value2.getInt();
                                case MUL -> value1.getInt() * value2.getInt();
                                case NE -> value1.ne(value2) ? 1 : 0;
                                case SUB -> value1.getInt() - value2.getInt();
                            }));
                        continue;
                    }
                    if (ir instanceof BVIR bVIR) {
                        if (bVIR.getLeft() instanceof Value value1 && bVIR.getRight() instanceof Value value2) {
                            if (switch (bVIR.getType()) {
                                case EQ -> value1.eq(value2);
                                case GE -> value1.ge(value2);
                                case GT -> value1.gt(value2);
                                case LE -> value1.le(value2);
                                case LT -> value1.lt(value2);
                                case NE -> value1.ne(value2);
                            })
                                newBlock.add(new JVIR(oldToNewMap.get(bVIR.getTrueBlock())));
                            else
                                newBlock.add(new JVIR(oldToNewMap.get(bVIR.getFalseBlock())));
                        } else {
                            newBlock.add(new BVIR(bVIR.getType(), bVIR.getLeft(), bVIR.getRight(),
                                    oldToNewMap.get(bVIR.getTrueBlock()), oldToNewMap.get(bVIR.getFalseBlock())));
                        }
                        continue;
                    }
                    if (ir instanceof JVIR jVIR) {
                        newBlock.add(new JVIR(oldToNewMap.get(jVIR.getBlock())));
                        continue;
                    }
                    if (ir instanceof UnaryVIR unaryVIR && unaryVIR.getSource() instanceof Value value) {
                        if (unaryVIR.getResult().getType() == Type.FLOAT)
                            newBlock.add(new LIVIR(unaryVIR.getResult(), switch (unaryVIR.getType()) {
                                case I2F -> (float) value.getInt();
                                case NEG -> -value.getFloat();
                                default -> throw new RuntimeException();
                            }));
                        else
                            newBlock.add(new LIVIR(unaryVIR.getResult(), switch (unaryVIR.getType()) {
                                case F2I -> (int) value.getFloat();
                                case L_NOT ->
                                        (value.getType() == Type.FLOAT ? (value.getFloat() == 0.0f) :
                                                (value.getInt() == 0)) ? 1 : 0;
                                case NEG -> -value.getInt();
                                default -> throw new RuntimeException();
                            }));
                        continue;
                    }
                    newBlock.add(ir);
                }
            }
            func.setBlocks(newBlocks);
        }
    }
}
