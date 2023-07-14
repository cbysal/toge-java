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
        boolean toContinue;
        do {
            toContinue = false;
            for (VirtualFunction func : funcs.values()) {
                List<Block> blocks = func.getBlocks();
                for (Block block : blocks) {
                    Map<VReg, Integer> regToValueMap = new HashMap<>();
                    for (int irId = 0; irId < block.size(); irId++) {
                        VIR ir = block.get(irId);
                        if (ir instanceof BinaryVIR binaryVIR) {
                            VIRItem left = binaryVIR.getLeft();
                            VIRItem right = binaryVIR.getRight();
                            if (left instanceof VReg reg && regToValueMap.containsKey(reg)) {
                                left = reg.getType() == Type.FLOAT ?
                                        new Value(Float.intBitsToFloat(regToValueMap.get(reg))) :
                                        new Value(regToValueMap.get(reg));
                                toContinue = true;
                            }
                            if (right instanceof VReg reg && regToValueMap.containsKey(reg)) {
                                right = reg.getType() == Type.FLOAT ?
                                        new Value(Float.intBitsToFloat(regToValueMap.get(reg))) :
                                        new Value(regToValueMap.get(reg));
                                toContinue = true;
                            }
                            block.set(irId, new BinaryVIR(binaryVIR.getType(), binaryVIR.getResult(), left, right));
                            regToValueMap.remove(binaryVIR.getResult());
                            continue;
                        }
                        if (ir instanceof CallVIR callVIR) {
                            List<VIRItem> params = callVIR.getParams();
                            for (int j = 0; j < params.size(); j++)
                                if (params.get(j) instanceof VReg reg && regToValueMap.containsKey(reg)) {
                                    params.set(j, reg.getType() == Type.FLOAT ?
                                            new Value(Float.intBitsToFloat(regToValueMap.get(reg))) :
                                            new Value(regToValueMap.get(reg)));
                                    toContinue = true;
                                }
                            regToValueMap.remove(callVIR.getRetVal());
                            continue;
                        }
                        if (ir instanceof LIVIR liVIR) {
                            regToValueMap.put(liVIR.getTarget(), liVIR.getValue());
                            continue;
                        }
                        if (ir instanceof LoadVIR loadVIR) {
                            List<VIRItem> dimensions = loadVIR.getDimensions();
                            for (int j = 0; j < dimensions.size(); j++)
                                if (dimensions.get(j) instanceof VReg reg && regToValueMap.containsKey(reg)) {
                                    dimensions.set(j, reg.getType() == Type.FLOAT ?
                                            new Value(Float.intBitsToFloat(regToValueMap.get(reg))) :
                                            new Value(regToValueMap.get(reg)));
                                    toContinue = true;
                                }
                            regToValueMap.remove(loadVIR.getTarget());
                            continue;
                        }
                        if (ir instanceof MovVIR movVIR) {
                            if (regToValueMap.containsKey(movVIR.getSource())) {
                                block.set(irId, new LIVIR(movVIR.getTarget(), regToValueMap.get(movVIR.getSource())));
                                toContinue = true;
                            }
                            regToValueMap.remove(movVIR.getTarget());
                            continue;
                        }
                        if (ir instanceof StoreVIR storeVIR) {
                            List<VIRItem> dimensions = storeVIR.getDimensions();
                            for (int j = 0; j < dimensions.size(); j++)
                                if (dimensions.get(j) instanceof VReg reg && regToValueMap.containsKey(reg)) {
                                    dimensions.set(j, reg.getType() == Type.FLOAT ?
                                            new Value(Float.intBitsToFloat(regToValueMap.get(reg))) :
                                            new Value(regToValueMap.get(reg)));
                                    toContinue = true;
                                }
                            continue;
                        }
                        if (ir instanceof UnaryVIR unaryVIR) {
                            if (unaryVIR.getSource() instanceof VReg reg && regToValueMap.containsKey(reg)) {
                                block.set(irId, new UnaryVIR(unaryVIR.getType(), unaryVIR.getResult(),
                                        reg.getType() == Type.FLOAT ?
                                                new Value(Float.intBitsToFloat(regToValueMap.get(reg))) :
                                                new Value(regToValueMap.get(reg))));
                                toContinue = true;
                            }
                            regToValueMap.remove(unaryVIR.getResult());
                            continue;
                        }
                    }
                    Map<Block.Cond, Block> newCondBlocks = new HashMap<>();
                    for (Map.Entry<Block.Cond, Block> entry : block.getCondBlocks().entrySet()) {
                        Block.Cond cond = entry.getKey();
                        VIRItem left = cond.left();
                        VIRItem right = cond.right();
                        Block targetBlock = entry.getValue();
                        if (left instanceof VReg reg && regToValueMap.containsKey(reg)) {
                            left = reg.getType() == Type.FLOAT ?
                                    new Value(Float.intBitsToFloat(regToValueMap.get(reg))) :
                                    new Value(regToValueMap.get(reg));
                            toContinue = true;
                        }
                        if (right instanceof VReg reg && regToValueMap.containsKey(reg)) {
                            right = reg.getType() == Type.FLOAT ?
                                    new Value(Float.intBitsToFloat(regToValueMap.get(reg))) :
                                    new Value(regToValueMap.get(reg));
                            toContinue = true;
                        }
                        newCondBlocks.put(new Block.Cond(cond.type(), left, right), targetBlock);
                    }
                    block.clearCondBlocks();
                    newCondBlocks.forEach(block::setCondBlock);
                }
            }
            standardize();
        } while (toContinue);
    }

    private void deadcodeElimination() {
        deadcodeEliminationOnReachability();
        deadcodeEliminationOnRoot();
    }

    private void deadcodeEliminationOnReachability() {
        for (VirtualFunction func : funcs.values()) {
            List<Block> blocks = func.getBlocks();
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
                curBlock.getCondBlocks().values().forEach(frontier::offer);
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
                for (Block.Cond cond : block.getCondBlocks().keySet()) {
                    if (cond.left() instanceof VReg reg)
                        usedRegs.add(reg);
                    if (cond.right() instanceof VReg reg)
                        usedRegs.add(reg);
                }
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
            List<Block> blocks = func.getBlocks();
            for (Block block : blocks) {
                for (int irId = 0; irId < block.size(); irId++) {
                    VIR ir = block.get(irId);
                    if (ir instanceof BinaryVIR binaryVIR) {
                        if (!usedRegs.contains(binaryVIR.getResult())) {
                            block.remove(irId);
                            irId--;
                        }
                        continue;
                    }
                    if (ir instanceof LIVIR liVIR) {
                        if (!usedRegs.contains(liVIR.getTarget())) {
                            block.remove(irId);
                            irId--;
                        }
                        continue;
                    }
                    if (ir instanceof LoadVIR loadVIR) {
                        if (!usedRegs.contains(loadVIR.getTarget())) {
                            block.remove(irId);
                            irId--;
                        }
                        continue;
                    }
                    if (ir instanceof MovVIR movVIR) {
                        if (!usedRegs.contains(movVIR.getTarget())) {
                            block.remove(irId);
                            irId--;
                        }
                        continue;
                    }
                    if (ir instanceof StoreVIR storeVIR) {
                        if (!usedSymbols.contains(storeVIR.getSymbol())) {
                            block.remove(irId);
                            irId--;
                        }
                        continue;
                    }
                    if (ir instanceof UnaryVIR unaryVIR) {
                        if (!usedRegs.contains(unaryVIR.getResult())) {
                            block.remove(irId);
                            irId--;
                        }
                        continue;
                    }
                }
            }
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
                            newBlock.setDefaultBlock(oldToNewMap.get(oldBlock.getDefaultBlock()));
                            for (Map.Entry<Block.Cond, Block> oldCondBlockEntry : oldBlock.getCondBlocks().entrySet()) {
                                Block.Cond oldCond = oldCondBlockEntry.getKey();
                                Block oldCondBlock = oldCondBlockEntry.getValue();
                                VIRItem left = oldCond.left();
                                if (left instanceof VReg reg) {
                                    if (regMap.containsKey(reg))
                                        left = regMap.get(reg);
                                    else {
                                        VReg newReg = new VReg(reg.getType());
                                        left = newReg;
                                        regMap.put(reg, newReg);
                                    }
                                }
                                VIRItem right = oldCond.right();
                                if (right instanceof VReg reg) {
                                    if (regMap.containsKey(reg))
                                        right = regMap.get(reg);
                                    else {
                                        VReg newReg = new VReg(reg.getType());
                                        right = newReg;
                                        regMap.put(reg, newReg);
                                    }
                                }
                                Block.Cond newCond = new Block.Cond(oldCond.type(), left, right);
                                Block newCondBlock = oldToNewMap.get(oldCondBlock);
                                newBlock.setCondBlock(newCond, newCondBlock);
                            }
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
                        lastBlock.setDefaultBlock(curBlock.getDefaultBlock());
                        curBlock.getCondBlocks().forEach(lastBlock::setCondBlock);
                        curBlock.setDefaultBlock(preCallBlock);
                        curBlock.clearCondBlocks();
                        preCallBlock.setDefaultBlock(newBlocks.get(0));
                        newBlocks.get(newBlocks.size() - 1).setDefaultBlock(lastBlock);
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
                if (curBlock.isEmpty() && curBlock.getCondBlocks().isEmpty() && curBlock.getDefaultBlock() == nextBlock) {
                    blocks.remove(blockId);
                    blockId--;
                    for (Block block : blocks) {
                        if (block.getDefaultBlock() == curBlock)
                            block.setDefaultBlock(nextBlock);
                        block.getCondBlocks().entrySet().forEach(entry -> {
                            if (entry.getValue() == curBlock)
                                entry.setValue(nextBlock);
                        });
                    }
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
                    if (curBlock.isEmpty() && curBlock.getCondBlocks().isEmpty() && curBlock.getDefaultBlock() == nextBlock) {
                        blocks.remove(blockId);
                        toContinue = true;
                        break;
                    }
                }
                if (toContinue) {
                    for (Block block : blocks) {
                        if (block.getDefaultBlock() == curBlock)
                            block.setDefaultBlock(nextBlock);
                        Block finalCurBlock = curBlock;
                        Block finalNextBlock = nextBlock;
                        block.getCondBlocks().entrySet().forEach(entry -> {
                            if (entry.getValue() == finalCurBlock)
                                entry.setValue(finalNextBlock);
                        });
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
            List<Block> blocks = func.getBlocks();
            for (Block block : blocks) {
                Map<Block.Cond, Block> newCondMap = new HashMap<>();
                for (Map.Entry<Block.Cond, Block> entry : block.getCondBlocks().entrySet()) {
                    Block.Cond cond = entry.getKey();
                    Block targetBlock = entry.getValue();
                    if (cond.left() instanceof Value value1 && cond.right() instanceof Value value2) {
                        if (switch (cond.type()) {
                            case EQ -> value1.eq(value2);
                            case GE -> value1.ge(value2);
                            case GT -> value1.gt(value2);
                            case LE -> value1.le(value2);
                            case LT -> value1.lt(value2);
                            case NE -> value1.ne(value2);
                        }) {
                            block.setDefaultBlock(targetBlock);
                        }
                    } else {
                        newCondMap.put(cond, targetBlock);
                    }
                }
                block.clearCondBlocks();
                newCondMap.forEach(block::setCondBlock);
                for (int irId = 0; irId < block.size(); irId++) {
                    VIR ir = block.get(irId);
                    if (ir instanceof BinaryVIR binaryVIR && binaryVIR.getLeft() instanceof Value value1 && binaryVIR.getRight() instanceof Value value2) {
                        if (binaryVIR.getResult().getType() == Type.FLOAT)
                            block.set(irId, new LIVIR(binaryVIR.getResult(), switch (binaryVIR.getType()) {
                                case ADD -> value1.getFloat() + value2.getFloat();
                                case DIV -> value1.getFloat() / value2.getFloat();
                                case MUL -> value1.getFloat() * value2.getFloat();
                                case SUB -> value1.getFloat() - value2.getFloat();
                                default -> throw new RuntimeException();
                            }));
                        else
                            block.set(irId, new LIVIR(binaryVIR.getResult(), switch (binaryVIR.getType()) {
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
                    if (ir instanceof UnaryVIR unaryVIR && unaryVIR.getSource() instanceof Value value) {
                        if (unaryVIR.getResult().getType() == Type.FLOAT)
                            block.set(irId, new LIVIR(unaryVIR.getResult(), switch (unaryVIR.getType()) {
                                case I2F -> (float) value.getInt();
                                case NEG -> -value.getFloat();
                                default -> throw new RuntimeException();
                            }));
                        else
                            block.set(irId, new LIVIR(unaryVIR.getResult(), switch (unaryVIR.getType()) {
                                case F2I -> (int) value.getFloat();
                                case L_NOT ->
                                        (value.getType() == Type.FLOAT ? (value.getFloat() == 0.0f) :
                                                (value.getInt() == 0)) ? 1 : 0;
                                case NEG -> -value.getInt();
                                default -> throw new RuntimeException();
                            }));
                        continue;
                    }
                }
            }
        }
    }
}
