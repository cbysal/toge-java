package compile.codegen.virgen;

import common.Pair;
import compile.codegen.virgen.vir.*;
import compile.symbol.*;

import java.util.*;
import java.util.stream.Collectors;

public class VIROptimizer {
    private boolean isProcessed = false;
    private final Set<GlobalSymbol> globals;
    private final Map<String, VirtualFunction> funcs;

    public VIROptimizer(Set<GlobalSymbol> globals, Map<String, VirtualFunction> funcs) {
        this.globals = globals;
        this.funcs = funcs;
    }

    private void checkIfIsProcessed() {
        if (isProcessed)
            return;
        isProcessed = true;
        boolean toContinue;
        do {
            toContinue = singleLocal2Reg();
            toContinue |= globalToImm();
            toContinue |= deadcodeElimination();
            toContinue |= splitGlobals();
            toContinue |= splitLocals();
            toContinue |= mergeBlocks();
            toContinue |= constPassForBlock();
            toContinue |= constPassForFunc();
            toContinue |= assignPass();
            toContinue |= deadcodeElimination();
            toContinue |= mergeBlocks();
            toContinue |= instrCombine();
            toContinue |= peekHoleForBlock();
            toContinue |= deadcodeElimination();
            toContinue |= matchPatterns();
            toContinue |= mergeBlocks();
        } while (toContinue);
        functionInline();
        do {
            toContinue = singleLocal2Reg();
            toContinue |= globalToImm();
            toContinue |= deadcodeElimination();
            toContinue |= splitGlobals();
            toContinue |= splitLocals();
            toContinue |= mergeBlocks();
            toContinue |= constPassForBlock();
            toContinue |= constPassForFunc();
            toContinue |= assignPass();
            toContinue |= deadcodeElimination();
            toContinue |= mergeBlocks();
            toContinue |= instrCombine();
            toContinue |= peekHoleForBlock();
            toContinue |= deadcodeElimination();
            toContinue |= matchPatterns();
            toContinue |= mergeBlocks();
        } while (toContinue);
    }

    private boolean matchPatterns() {
        boolean modified = matchAbs();
        return modified;
    }

    private boolean matchAbs() {
        boolean modified = false;
        for (VirtualFunction func : funcs.values()) {
            List<Block> blocks = func.getBlocks();
            for (int i = 0; i < blocks.size(); i++) {
                Block block = blocks.get(i);
                Map<VReg, VReg> movMap = new HashMap<>();
                for (VIR ir : block)
                    if (ir instanceof MovVIR movVIR)
                        movMap.put(movVIR.getTarget(), movVIR.getSource());
                if (block.getCondBlocks().size() == 1) {
                    Block.Cond cond = block.getCondBlocks().get(0).first();
                    VReg passReg;
                    if ((cond.type() == Block.Cond.Type.LE || cond.type() == Block.Cond.Type.LT) && cond.left() instanceof VReg reg && cond.right() instanceof Value value) {
                        passReg = reg;
                    } else if ((cond.type() == Block.Cond.Type.GE || cond.type() == Block.Cond.Type.GT) && cond.left() instanceof Value value && cond.right() instanceof VReg reg) {
                        passReg = reg;
                    } else
                        continue;
                    Block nextBlock1 = block.getCondBlocks().get(0).second();
                    Block nextBlock2 = block.getDefaultBlock();
                    if (nextBlock1.size() == 1 && nextBlock1.getCondBlocks().isEmpty() && nextBlock1.getDefaultBlock() == nextBlock2) {
                        if (nextBlock1.get(0) instanceof UnaryVIR unaryVIR && unaryVIR.getType() == UnaryVIR.Type.NEG && unaryVIR.getResult() == unaryVIR.getSource() && movMap.get(unaryVIR.getResult()) == passReg) {
                            block.add(new UnaryVIR(UnaryVIR.Type.ABS, unaryVIR.getResult(), unaryVIR.getSource()));
                            block.getCondBlocks().clear();
                            blocks.remove(nextBlock1);
                            i--;
                            modified = true;
                        }
                    }
                }
            }
        }
        return modified;
    }

    private boolean peekHoleForBlock() {
        boolean modified = false;
        for (VirtualFunction func : funcs.values()) {
            for (Block block : func.getBlocks()) {
                for (int i = 0; i < block.size() - 1; i++) {
                    VIR ir1 = block.get(i);
                    VIR ir2 = block.get(i + 1);
                    if (ir1 instanceof BinaryVIR binaryVIR && ir2 instanceof MovVIR movVIR) {
                        if (binaryVIR.getLeft() instanceof VReg source1) {
                            VReg source2 = movVIR.getSource();
                            VReg target1 = binaryVIR.getResult();
                            VReg target2 = movVIR.getTarget();
                            if (target1 != source1 && target2 == source1 && target1 == source2) {
                                block.set(i, new BinaryVIR(binaryVIR.getType(), target2, source1,
                                        binaryVIR.getRight()));
                                block.set(i + 1, new MovVIR(target1, source1));
                                modified = true;
                            }
                        }
                        continue;
                    }
                    if (ir1 instanceof UnaryVIR unaryVIR && ir2 instanceof MovVIR movVIR) {
                        if (unaryVIR.getSource() instanceof VReg source1) {
                            VReg source2 = movVIR.getSource();
                            VReg target1 = unaryVIR.getResult();
                            VReg target2 = movVIR.getTarget();
                            if (target1 != source1 && target2 == source1 && target1 == source2) {
                                block.set(i, new UnaryVIR(unaryVIR.getType(), target2, source1));
                                block.set(i + 1, new MovVIR(target1, source1));
                                modified = true;
                            }
                        }
                        continue;
                    }
                    if (ir1 instanceof StoreVIR storeVIR && ir2 instanceof LoadVIR loadVIR) {
                        if (storeVIR.getSymbol() == loadVIR.getSymbol() && storeVIR.getDimensions().equals(loadVIR.getDimensions())) {
                            block.set(i + 1, new MovVIR(loadVIR.getTarget(), storeVIR.getSource()));
                            modified = true;
                        }
                        continue;
                    }
                    if (ir1 instanceof BinaryVIR binaryVIR1 && ir2 instanceof BinaryVIR binaryVIR2) {
                        if (binaryVIR1.getType() == BinaryVIR.Type.ADD && binaryVIR2.getType() == BinaryVIR.Type.SUB) {
                            if (binaryVIR1.getRight().equals(binaryVIR2.getRight()) && binaryVIR1.getLeft() instanceof VReg source1 && binaryVIR2.getLeft() instanceof VReg source2) {
                                VReg target1 = binaryVIR1.getResult();
                                VReg target2 = binaryVIR2.getResult();
                                if (target1 == source2 && target2 != source1) {
                                    block.set(i, new MovVIR(target2, source1));
                                    block.set(i + 1, binaryVIR1);
                                    modified = true;
                                }
                            }
                        }
                        continue;
                    }
                }
            }
        }
        return modified;
    }

    private boolean splitLocals() {
        boolean modified = false;
        for (VirtualFunction func : funcs.values()) {
            Set<LocalSymbol> locals =
                    func.getLocals().stream().filter(local -> !local.isSingle()).collect(Collectors.toSet());
            for (Block block : func.getBlocks()) {
                for (VIR ir : block) {
                    if (ir instanceof LoadVIR loadVIR && loadVIR.getSymbol() instanceof LocalSymbol local && locals.contains(local)) {
                        List<VIRItem> dimensions = loadVIR.getDimensions();
                        if (dimensions.isEmpty() || dimensions.get(0) instanceof VReg)
                            locals.remove(local);
                        continue;
                    }
                    if (ir instanceof StoreVIR storeVIR && storeVIR.getSymbol() instanceof LocalSymbol local && locals.contains(local)) {
                        List<VIRItem> dimensions = storeVIR.getDimensions();
                        if (dimensions.isEmpty() || dimensions.get(0) instanceof VReg)
                            locals.remove(local);
                        continue;
                    }
                }
            }
            func.getLocals().removeAll(locals);
            Map<LocalSymbol, List<LocalSymbol>> newLocalMap = new HashMap<>();
            for (LocalSymbol local : locals) {
                List<LocalSymbol> newLocals = new ArrayList<>();
                newLocalMap.put(local, newLocals);
                if (local.getDimensions().size() == 1) {
                    int dimension = local.getDimensions().get(0);
                    for (int i = 0; i < dimension; i++)
                        newLocals.add(new LocalSymbol(local.getType(), local.getName() + "." + i));
                } else {
                    List<Integer> oldDimensions = local.getDimensions();
                    for (int i = 0; i < oldDimensions.get(0); i++) {
                        newLocals.add(new LocalSymbol(local.getType(), local.getName() + "." + i,
                                oldDimensions.subList(1, oldDimensions.size())));
                    }
                }
            }
            newLocalMap.values().forEach(func.getLocals()::addAll);
            for (Block block : func.getBlocks()) {
                for (int i = 0; i < block.size(); i++) {
                    VIR ir = block.get(i);
                    if (ir instanceof LoadVIR loadVIR && loadVIR.getSymbol() instanceof LocalSymbol local && newLocalMap.containsKey(local)) {
                        List<VIRItem> dimensions = loadVIR.getDimensions();
                        block.set(i, new LoadVIR(loadVIR.getTarget(), dimensions.subList(1, dimensions.size()),
                                newLocalMap.get(local).get(((Value) dimensions.get(0)).getInt())));
                        continue;
                    }
                    if (ir instanceof StoreVIR storeVIR && storeVIR.getSymbol() instanceof LocalSymbol local && newLocalMap.containsKey(local)) {
                        List<VIRItem> dimensions = storeVIR.getDimensions();
                        block.set(i, new StoreVIR(newLocalMap.get(local).get(((Value) dimensions.get(0)).getInt()),
                                dimensions.subList(1, dimensions.size()), storeVIR.getSource()));
                        continue;
                    }
                }
            }
            modified |= !locals.isEmpty();
        }
        return modified;
    }

    private boolean splitGlobals() {
        Set<GlobalSymbol> globals =
                this.globals.stream().filter(global -> !global.isSingle()).collect(Collectors.toSet());
        for (VirtualFunction func : funcs.values()) {
            for (Block block : func.getBlocks()) {
                for (VIR ir : block) {
                    if (ir instanceof LoadVIR loadVIR && loadVIR.getSymbol() instanceof GlobalSymbol global && globals.contains(global)) {
                        List<VIRItem> dimensions = loadVIR.getDimensions();
                        if (dimensions.isEmpty() || dimensions.get(0) instanceof VReg)
                            globals.remove(global);
                        continue;
                    }
                    if (ir instanceof StoreVIR storeVIR && storeVIR.getSymbol() instanceof GlobalSymbol global && globals.contains(global)) {
                        List<VIRItem> dimensions = storeVIR.getDimensions();
                        if (dimensions.isEmpty() || dimensions.get(0) instanceof VReg)
                            globals.remove(global);
                        continue;
                    }
                }
            }
        }
        this.globals.removeAll(globals);
        Map<GlobalSymbol, List<GlobalSymbol>> newGlobalMap = new HashMap<>();
        for (GlobalSymbol global : globals) {
            List<GlobalSymbol> newGlobals = new ArrayList<>();
            newGlobalMap.put(global, newGlobals);
            if (global.getDimensions().size() == 1) {
                int dimension = global.getDimensions().get(0);
                for (int i = 0; i < dimension; i++) {
                    if (global.getType() == Type.INT)
                        newGlobals.add(new GlobalSymbol(global.isConst(), global.getType(),
                                global.getName() + "." + i, global.getInt(i)));
                    else
                        newGlobals.add(new GlobalSymbol(global.isConst(), global.getType(),
                                global.getName() + "." + i, global.getFloat(i)));
                }
            } else {
                List<Integer> oldDimensions = global.getDimensions();
                List<Map<Integer, Integer>> initMapList = new ArrayList<>();
                for (int i = 0; i < oldDimensions.get(0); i++)
                    initMapList.add(new HashMap<>());
                Map<Integer, Integer> initMap = global.getValues();
                int newSize = global.size() / 4 / oldDimensions.get(0);
                for (Map.Entry<Integer, Integer> entry : initMap.entrySet()) {
                    int index = entry.getKey() / newSize;
                    initMapList.get(index).put(entry.getKey() % newSize, entry.getValue());
                }
                for (int i = 0; i < oldDimensions.get(0); i++) {
                    newGlobals.add(new GlobalSymbol(global.isConst(), global.getType(), global.getName() + "." + i,
                            oldDimensions.subList(1, oldDimensions.size()), initMapList.get(i)));
                }
            }
        }
        newGlobalMap.values().forEach(this.globals::addAll);
        for (VirtualFunction func : funcs.values()) {
            for (Block block : func.getBlocks()) {
                for (int i = 0; i < block.size(); i++) {
                    VIR ir = block.get(i);
                    if (ir instanceof LoadVIR loadVIR && loadVIR.getSymbol() instanceof GlobalSymbol global && newGlobalMap.containsKey(global)) {
                        List<VIRItem> dimensions = loadVIR.getDimensions();
                        block.set(i, new LoadVIR(loadVIR.getTarget(), dimensions.subList(1, dimensions.size()),
                                newGlobalMap.get(global).get(((Value) dimensions.get(0)).getInt())));
                        continue;
                    }
                    if (ir instanceof StoreVIR storeVIR && storeVIR.getSymbol() instanceof GlobalSymbol global && newGlobalMap.containsKey(global)) {
                        List<VIRItem> dimensions = storeVIR.getDimensions();
                        block.set(i, new StoreVIR(newGlobalMap.get(global).get(((Value) dimensions.get(0)).getInt()),
                                dimensions.subList(1, dimensions.size()), storeVIR.getSource()));
                        continue;
                    }
                }
            }
        }
        return !globals.isEmpty();
    }

    private boolean instrCombine() {
        boolean modified = false;
        for (VirtualFunction func : funcs.values()) {
            for (Block block : func.getBlocks()) {
                Map<VReg, Pair<VReg, Value>> mulIRs = new HashMap<>();
                for (int i = 0; i < block.size(); i++) {
                    VIR ir = block.get(i);
                    if (ir instanceof BinaryVIR binaryVIR) {
                        if (binaryVIR.getType() == BinaryVIR.Type.ADD) {
                            if (binaryVIR.getLeft() instanceof VReg reg1 && binaryVIR.getRight() instanceof VReg reg2) {
                                if (reg1 == reg2) {
                                    block.set(i, new BinaryVIR(BinaryVIR.Type.MUL, binaryVIR.getResult(), reg1,
                                            new Value(2)));
                                    mulIRs.put(binaryVIR.getResult(), new Pair<>(reg1, new Value(2)));
                                    modified = true;
                                    continue;
                                }
                                if (mulIRs.containsKey(reg1)) {
                                    Pair<VReg, Value> regValue = mulIRs.get(reg1);
                                    if (regValue.first() == reg2) {
                                        block.set(i, new BinaryVIR(BinaryVIR.Type.MUL, binaryVIR.getResult(),
                                                regValue.first(), new Value(regValue.second().getInt() + 1)));
                                        mulIRs.put(binaryVIR.getResult(), new Pair<>(regValue.first(),
                                                new Value(regValue.second().getInt() + 1)));
                                        modified = true;
                                    } else {
                                        mulIRs.remove(binaryVIR.getResult());
                                    }
                                    continue;
                                }
                                if (mulIRs.containsKey(reg2)) {
                                    Pair<VReg, Value> regValue = mulIRs.get(reg2);
                                    if (regValue.first() == reg1) {
                                        block.set(i, new BinaryVIR(BinaryVIR.Type.MUL, binaryVIR.getResult(),
                                                regValue.first(), new Value(regValue.second().getInt() + 1)));
                                        mulIRs.put(binaryVIR.getResult(), new Pair<>(regValue.first(),
                                                new Value(regValue.second().getInt() + 1)));
                                        modified = true;
                                    } else {
                                        mulIRs.remove(binaryVIR.getResult());
                                    }
                                    continue;
                                }
                            }
                            mulIRs.remove(binaryVIR.getResult());
                        } else if (binaryVIR.getType() == BinaryVIR.Type.MUL) {
                            if (binaryVIR.getLeft() instanceof VReg reg && binaryVIR.getRight() instanceof Value value) {
                                mulIRs.put(binaryVIR.getResult(), new Pair<>(reg, value));
                            } else if (binaryVIR.getLeft() instanceof Value value && binaryVIR.getRight() instanceof VReg reg) {
                                mulIRs.put(binaryVIR.getResult(), new Pair<>(reg, value));
                            }
                        } else if (binaryVIR.getType() == BinaryVIR.Type.DIV) {
                            if (binaryVIR.getLeft() instanceof VReg reg && binaryVIR.getRight() instanceof Value value && mulIRs.containsKey(reg)) {
                                Pair<VReg, Value> regValue = mulIRs.get(reg);
                                if (regValue.second().equals(value)) {
                                    block.set(i, new MovVIR(binaryVIR.getResult(), regValue.first()));
                                    modified = true;
                                }
                            }
                            mulIRs.remove(binaryVIR.getResult());
                        } else {
                            mulIRs.remove(binaryVIR.getResult());
                        }
                        continue;
                    }
                    if (ir instanceof CallVIR callVIR) {
                        if (callVIR.getRetVal() != null)
                            mulIRs.remove(callVIR.getRetVal());
                        continue;
                    }
                    if (ir instanceof LIVIR liVIR) {
                        mulIRs.remove(liVIR.getTarget());
                        continue;
                    }
                    if (ir instanceof LoadVIR loadVIR) {
                        mulIRs.remove(loadVIR.getTarget());
                        continue;
                    }
                    if (ir instanceof MovVIR movVIR) {
                        mulIRs.remove(movVIR.getTarget());
                        continue;
                    }
                    if (ir instanceof UnaryVIR unaryVIR) {
                        mulIRs.remove(unaryVIR.getResult());
                        continue;
                    }
                }
            }
        }
        return modified;
    }

    private boolean globalToImm() {
        Set<GlobalSymbol> toRemoveGlobals = globals.stream().filter(DataSymbol::isSingle).collect(Collectors.toSet());
        for (VirtualFunction func : funcs.values())
            for (Block block : func.getBlocks())
                for (VIR ir : block)
                    if (ir instanceof StoreVIR storeVIR && storeVIR.getSymbol() instanceof GlobalSymbol global && global.isSingle())
                        toRemoveGlobals.remove(global);
        for (VirtualFunction func : funcs.values())
            for (Block block : func.getBlocks())
                for (int i = 0; i < block.size(); i++) {
                    VIR ir = block.get(i);
                    if (ir instanceof LoadVIR loadVIR && loadVIR.getSymbol() instanceof GlobalSymbol global && toRemoveGlobals.contains(global)) {
                        if (global.getType() == Type.INT)
                            block.set(i, new LIVIR(loadVIR.getTarget(), global.getInt()));
                        else
                            block.set(i, new LIVIR(loadVIR.getTarget(), global.getFloat()));
                    }
                }
        globals.removeAll(toRemoveGlobals);
        return !toRemoveGlobals.isEmpty();
    }

    private boolean assignPass() {
        boolean modified = false;
        boolean toContinue;
        do {
            toContinue = false;
            for (VirtualFunction func : funcs.values()) {
                List<Block> blocks = func.getBlocks();
                for (Block block : blocks) {
                    Map<VReg, VReg> regToRegMap = new HashMap<>();
                    for (int irId = 0; irId < block.size(); irId++) {
                        VIR ir = block.get(irId);
                        if (ir instanceof BinaryVIR binaryVIR) {
                            VIRItem left = binaryVIR.getLeft();
                            VIRItem right = binaryVIR.getRight();
                            if (left instanceof VReg reg && regToRegMap.containsKey(reg)) {
                                left = regToRegMap.get(reg);
                                toContinue = true;
                                modified = true;
                            }
                            if (right instanceof VReg reg && regToRegMap.containsKey(reg)) {
                                right = regToRegMap.get(reg);
                                toContinue = true;
                                modified = true;
                            }
                            block.set(irId, new BinaryVIR(binaryVIR.getType(), binaryVIR.getResult(), left, right));
                            regToRegMap.remove(binaryVIR.getResult());
                            regToRegMap =
                                    regToRegMap.entrySet().stream().filter(entry -> entry.getValue() != binaryVIR.getResult()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                            continue;
                        }
                        if (ir instanceof CallVIR callVIR) {
                            List<VIRItem> params = callVIR.getParams();
                            for (int j = 0; j < params.size(); j++)
                                if (params.get(j) instanceof VReg reg && regToRegMap.containsKey(reg)) {
                                    params.set(j, regToRegMap.get(reg));
                                    toContinue = true;
                                    modified = true;
                                }
                            regToRegMap.remove(callVIR.getRetVal());
                            regToRegMap =
                                    regToRegMap.entrySet().stream().filter(entry -> entry.getValue() != callVIR.getRetVal()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                            continue;
                        }
                        if (ir instanceof LIVIR liVIR) {
                            regToRegMap.remove(liVIR.getTarget());
                            regToRegMap =
                                    regToRegMap.entrySet().stream().filter(entry -> entry.getValue() != liVIR.getTarget()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                            continue;
                        }
                        if (ir instanceof LoadVIR loadVIR) {
                            List<VIRItem> dimensions = loadVIR.getDimensions();
                            for (int j = 0; j < dimensions.size(); j++)
                                if (dimensions.get(j) instanceof VReg reg && regToRegMap.containsKey(reg)) {
                                    dimensions.set(j, regToRegMap.get(reg));
                                    toContinue = true;
                                    modified = true;
                                }
                            regToRegMap.remove(loadVIR.getTarget());
                            regToRegMap =
                                    regToRegMap.entrySet().stream().filter(entry -> entry.getValue() != loadVIR.getTarget()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                            continue;
                        }
                        if (ir instanceof MovVIR movVIR) {
                            if (regToRegMap.containsKey(movVIR.getSource())) {
                                regToRegMap.put(movVIR.getTarget(), regToRegMap.get(movVIR.getSource()));
                                block.set(irId, new MovVIR(movVIR.getTarget(), regToRegMap.get(movVIR.getSource())));
                                toContinue = true;
                                modified = true;
                            } else {
                                regToRegMap.put(movVIR.getTarget(), movVIR.getSource());
                            }
                            regToRegMap =
                                    regToRegMap.entrySet().stream().filter(entry -> entry.getValue() != movVIR.getTarget()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                            continue;
                        }
                        if (ir instanceof StoreVIR storeVIR) {
                            List<VIRItem> dimensions = storeVIR.getDimensions();
                            for (int j = 0; j < dimensions.size(); j++)
                                if (dimensions.get(j) instanceof VReg reg && regToRegMap.containsKey(reg)) {
                                    dimensions.set(j, regToRegMap.get(reg));
                                    toContinue = true;
                                    modified = true;
                                }
                            if (regToRegMap.containsKey(storeVIR.getSource())) {
                                block.set(irId, new StoreVIR(storeVIR.getSymbol(), dimensions,
                                        regToRegMap.get(storeVIR.getSource())));
                                toContinue = true;
                                modified = true;
                            }
                            continue;
                        }
                        if (ir instanceof UnaryVIR unaryVIR) {
                            if (unaryVIR.getSource() instanceof VReg reg && regToRegMap.containsKey(reg)) {
                                block.set(irId, new UnaryVIR(unaryVIR.getType(), unaryVIR.getResult(),
                                        regToRegMap.get(reg)));
                                toContinue = true;
                                modified = true;
                            }
                            regToRegMap.remove(unaryVIR.getResult());
                            regToRegMap =
                                    regToRegMap.entrySet().stream().filter(entry -> entry.getValue() != unaryVIR.getResult()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                            continue;
                        }
                    }
                    Map<Block.Cond, Block> newCondBlocks = new HashMap<>();
                    for (Pair<Block.Cond, Block> condBlock : block.getCondBlocks()) {
                        Block.Cond cond = condBlock.first();
                        VIRItem left = cond.left();
                        VIRItem right = cond.right();
                        Block targetBlock = condBlock.second();
                        if (left instanceof VReg reg && regToRegMap.containsKey(reg)) {
                            left = regToRegMap.get(reg);
                            toContinue = true;
                            modified = true;
                        }
                        if (right instanceof VReg reg && regToRegMap.containsKey(reg)) {
                            right = regToRegMap.get(reg);
                            toContinue = true;
                            modified = true;
                        }
                        newCondBlocks.put(new Block.Cond(cond.type(), left, right), targetBlock);
                    }
                    block.clearCondBlocks();
                    newCondBlocks.forEach(block::setCondBlock);
                }
            }
            standardize();
        } while (toContinue);
        return modified;
    }

    private boolean constPassForBlock() {
        boolean modified = false;
        boolean toContinue;
        do {
            toContinue = false;
            for (VirtualFunction func : funcs.values()) {
                List<Block> blocks = func.getBlocks();
                for (Block block : blocks) {
                    Map<GlobalSymbol, VReg> globalToRegMap = new HashMap<>();
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
                                modified = true;
                            }
                            if (right instanceof VReg reg && regToValueMap.containsKey(reg)) {
                                right = reg.getType() == Type.FLOAT ?
                                        new Value(Float.intBitsToFloat(regToValueMap.get(reg))) :
                                        new Value(regToValueMap.get(reg));
                                toContinue = true;
                                modified = true;
                            }
                            block.set(irId, new BinaryVIR(binaryVIR.getType(), binaryVIR.getResult(), left, right));
                            regToValueMap.remove(binaryVIR.getResult());
                            globalToRegMap =
                                    globalToRegMap.entrySet().stream().filter(entry -> entry.getValue() != binaryVIR.getResult()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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
                                    modified = true;
                                }
                            if (callVIR.getRetVal() != null) {
                                regToValueMap.remove(callVIR.getRetVal());
                                globalToRegMap =
                                        globalToRegMap.entrySet().stream().filter(entry -> entry.getValue() != callVIR.getRetVal()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                            }
                            continue;
                        }
                        if (ir instanceof LIVIR liVIR) {
                            regToValueMap.put(liVIR.getTarget(), liVIR.second());
                            globalToRegMap =
                                    globalToRegMap.entrySet().stream().filter(entry -> entry.getValue() != liVIR.getTarget()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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
                                    modified = true;
                                }
                            if (loadVIR.getSymbol() instanceof GlobalSymbol global && globalToRegMap.containsKey(global))
                                block.set(irId, new MovVIR(loadVIR.getTarget(), globalToRegMap.get(global)));
                            regToValueMap.remove(loadVIR.getTarget());
                            globalToRegMap =
                                    globalToRegMap.entrySet().stream().filter(entry -> entry.getValue() != loadVIR.getTarget()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                            continue;
                        }
                        if (ir instanceof MovVIR movVIR) {
                            if (regToValueMap.containsKey(movVIR.getSource())) {
                                block.set(irId, new LIVIR(movVIR.getTarget(), regToValueMap.get(movVIR.getSource())));
                                toContinue = true;
                                modified = true;
                            }
                            regToValueMap.remove(movVIR.getTarget());
                            globalToRegMap =
                                    globalToRegMap.entrySet().stream().filter(entry -> entry.getValue() != movVIR.getTarget()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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
                                    modified = true;
                                }
                            if (storeVIR.getSymbol() instanceof GlobalSymbol global && global.isSingle())
                                globalToRegMap.put(global, storeVIR.getSource());
                            continue;
                        }
                        if (ir instanceof UnaryVIR unaryVIR) {
                            if (unaryVIR.getSource() instanceof VReg reg && regToValueMap.containsKey(reg)) {
                                block.set(irId, new UnaryVIR(unaryVIR.getType(), unaryVIR.getResult(),
                                        reg.getType() == Type.FLOAT ?
                                                new Value(Float.intBitsToFloat(regToValueMap.get(reg))) :
                                                new Value(regToValueMap.get(reg))));
                                toContinue = true;
                                modified = true;
                            }
                            regToValueMap.remove(unaryVIR.getResult());
                            globalToRegMap =
                                    globalToRegMap.entrySet().stream().filter(entry -> entry.getValue() != unaryVIR.getResult()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                            continue;
                        }
                    }
                    Map<Block.Cond, Block> newCondBlocks = new HashMap<>();
                    for (Pair<Block.Cond, Block> entry : block.getCondBlocks()) {
                        Block.Cond cond = entry.first();
                        VIRItem left = cond.left();
                        VIRItem right = cond.right();
                        Block targetBlock = entry.second();
                        if (left instanceof VReg reg && regToValueMap.containsKey(reg)) {
                            left = reg.getType() == Type.FLOAT ?
                                    new Value(Float.intBitsToFloat(regToValueMap.get(reg))) :
                                    new Value(regToValueMap.get(reg));
                            toContinue = true;
                            modified = true;
                        }
                        if (right instanceof VReg reg && regToValueMap.containsKey(reg)) {
                            right = reg.getType() == Type.FLOAT ?
                                    new Value(Float.intBitsToFloat(regToValueMap.get(reg))) :
                                    new Value(regToValueMap.get(reg));
                            toContinue = true;
                            modified = true;
                        }
                        newCondBlocks.put(new Block.Cond(cond.type(), left, right), targetBlock);
                    }
                    block.clearCondBlocks();
                    newCondBlocks.forEach(block::setCondBlock);
                }
            }
            standardize();
        } while (toContinue);
        return modified;
    }

    private boolean constPassForFunc() {
        boolean modified = false;
        boolean toContinue;
        do {
            toContinue = false;
            for (VirtualFunction func : funcs.values()) {
                List<Block> blocks = func.getBlocks();
                Map<VReg, Integer> writeCounter = new HashMap<>();
                Map<VReg, Integer> regToValueMap = new HashMap<>();
                for (Block block : blocks) {
                    for (VIR ir : block) {
                        if (ir instanceof BinaryVIR binaryVIR) {
                            writeCounter.put(binaryVIR.getResult(), writeCounter.getOrDefault(binaryVIR.getResult(),
                                    0) + 1);
                            continue;
                        }
                        if (ir instanceof CallVIR callVIR) {
                            if (callVIR.getRetVal() != null)
                                writeCounter.put(callVIR.getRetVal(), writeCounter.getOrDefault(callVIR.getRetVal(),
                                        0) + 1);
                            continue;
                        }
                        if (ir instanceof LIVIR liVIR) {
                            writeCounter.put(liVIR.getTarget(), writeCounter.getOrDefault(liVIR.getTarget(), 0) + 1);
                            regToValueMap.put(liVIR.getTarget(), liVIR.second());
                            continue;
                        }
                        if (ir instanceof LoadVIR loadVIR) {
                            writeCounter.put(loadVIR.getTarget(),
                                    writeCounter.getOrDefault(loadVIR.getTarget(), 0) + 1);
                            continue;
                        }
                        if (ir instanceof MovVIR movVIR) {
                            writeCounter.put(movVIR.getTarget(), writeCounter.getOrDefault(movVIR.getTarget(), 0) + 1);
                            continue;
                        }
                        if (ir instanceof UnaryVIR unaryVIR) {
                            writeCounter.put(unaryVIR.getResult(),
                                    writeCounter.getOrDefault(unaryVIR.getResult(), 0) + 1);
                            continue;
                        }
                    }
                }
                for (Map.Entry<VReg, Integer> entry : writeCounter.entrySet())
                    if (entry.getValue() != 1)
                        regToValueMap.remove(entry.getKey());
                for (Block block : blocks) {
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
                                modified = true;
                            }
                            if (right instanceof VReg reg && regToValueMap.containsKey(reg)) {
                                right = reg.getType() == Type.FLOAT ?
                                        new Value(Float.intBitsToFloat(regToValueMap.get(reg))) :
                                        new Value(regToValueMap.get(reg));
                                toContinue = true;
                                modified = true;
                            }
                            block.set(irId, new BinaryVIR(binaryVIR.getType(), binaryVIR.getResult(), left, right));
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
                                    modified = true;
                                }
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
                                    modified = true;
                                }
                            continue;
                        }
                        if (ir instanceof MovVIR movVIR) {
                            if (regToValueMap.containsKey(movVIR.getSource())) {
                                block.set(irId, new LIVIR(movVIR.getTarget(), regToValueMap.get(movVIR.getSource())));
                                toContinue = true;
                                modified = true;
                            }
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
                                    modified = true;
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
                                modified = true;
                            }
                            continue;
                        }
                    }
                    Map<Block.Cond, Block> newCondBlocks = new HashMap<>();
                    for (Pair<Block.Cond, Block> entry : block.getCondBlocks()) {
                        Block.Cond cond = entry.first();
                        VIRItem left = cond.left();
                        VIRItem right = cond.right();
                        Block targetBlock = entry.second();
                        if (left instanceof VReg reg && regToValueMap.containsKey(reg)) {
                            left = reg.getType() == Type.FLOAT ?
                                    new Value(Float.intBitsToFloat(regToValueMap.get(reg))) :
                                    new Value(regToValueMap.get(reg));
                            toContinue = true;
                            modified = true;
                        }
                        if (right instanceof VReg reg && regToValueMap.containsKey(reg)) {
                            right = reg.getType() == Type.FLOAT ?
                                    new Value(Float.intBitsToFloat(regToValueMap.get(reg))) :
                                    new Value(regToValueMap.get(reg));
                            toContinue = true;
                            modified = true;
                        }
                        newCondBlocks.put(new Block.Cond(cond.type(), left, right), targetBlock);
                    }
                    block.clearCondBlocks();
                    newCondBlocks.forEach(block::setCondBlock);
                }
            }
            standardize();
        } while (toContinue);
        return modified;
    }

    private boolean deadcodeElimination() {
        boolean modified = deadcodeEliminationOnReachability();
        modified |= deadcodeEliminationOnRoot();
        return modified;
    }

    private boolean deadcodeEliminationOnReachability() {
        boolean modified = false;
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
                curBlock.getCondBlocks().stream().map(Pair::second).filter(Objects::nonNull).forEach(frontier::offer);
            }
            List<Block> newBlocks = blocks.stream().filter(reachableBlocks::contains).collect(Collectors.toList());
            modified |= func.getBlocks().size() != newBlocks.size();
            func.setBlocks(newBlocks);
        }
        return modified;
    }

    private boolean deadcodeEliminationOnRoot() {
        Set<VReg> regFrontier = new HashSet<>();
        Set<Symbol> symbolFrontier = new HashSet<>();
        Map<VReg, Set<VReg>> regToRegMap = new HashMap<>();
        Map<VReg, Set<Symbol>> regToSymbolMap = new HashMap<>();
        Map<Symbol, Set<VReg>> symbolToRegMap = new HashMap<>();
        for (VirtualFunction func : funcs.values()) {
            if (func.getRetVal() != null)
                regFrontier.add(func.getRetVal());
            symbolFrontier.addAll(func.getSymbol().getParams());
            List<Block> blocks = func.getBlocks();
            for (Block block : blocks) {
                for (Pair<Block.Cond, Block> condBlock : block.getCondBlocks()) {
                    if (condBlock.first().left() instanceof VReg reg)
                        regFrontier.add(reg);
                    if (condBlock.first().right() instanceof VReg reg)
                        regFrontier.add(reg);
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
                                regFrontier.add(reg);
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
        Set<VReg> usedRegs = new HashSet<>(regFrontier);
        Set<Symbol> usedSymbols = new HashSet<>(symbolFrontier);
        while (!regFrontier.isEmpty() || !symbolFrontier.isEmpty()) {
            Set<VReg> newRegFrontier = new HashSet<>();
            Set<Symbol> newSymbolFrontier = new HashSet<>();
            for (VReg reg : regFrontier) {
                for (VReg newReg : regToRegMap.getOrDefault(reg, Set.of()))
                    if (!usedRegs.contains(newReg)) {
                        usedRegs.add(newReg);
                        newRegFrontier.add(newReg);
                    }
                for (Symbol newSymbol : regToSymbolMap.getOrDefault(reg, Set.of()))
                    if (!usedSymbols.contains(newSymbol)) {
                        usedSymbols.add(newSymbol);
                        symbolFrontier.add(newSymbol);
                    }
            }
            for (Symbol symbol : symbolFrontier)
                for (VReg newReg : symbolToRegMap.getOrDefault(symbol, Set.of()))
                    if (!usedRegs.contains(newReg)) {
                        usedRegs.add(newReg);
                        newRegFrontier.add(newReg);
                    }
            regFrontier = newRegFrontier;
            symbolFrontier = newSymbolFrontier;
        }
        boolean modified =
                globals.size() != usedSymbols.stream().filter(symbol -> symbol instanceof GlobalSymbol).count();
        globals.clear();
        globals.addAll(usedSymbols.stream().filter(symbol -> symbol instanceof GlobalSymbol).map(symbol -> (GlobalSymbol) symbol).collect(Collectors.toSet()));
        for (VirtualFunction func : funcs.values()) {
            List<Block> blocks = func.getBlocks();
            modified |= func.getLocals().size() != usedSymbols.stream().filter(symbol -> symbol instanceof LocalSymbol local && func.getLocals().contains(local)).count();
            func.setLocals(usedSymbols.stream().filter(symbol -> symbol instanceof LocalSymbol local && func.getLocals().contains(local)).map(symbol -> (LocalSymbol) symbol).collect(Collectors.toSet()));
            for (Block block : blocks) {
                for (int irId = 0; irId < block.size(); irId++) {
                    VIR ir = block.get(irId);
                    if (ir instanceof BinaryVIR binaryVIR) {
                        if (!usedRegs.contains(binaryVIR.getResult())) {
                            block.remove(irId);
                            modified = true;
                            irId--;
                        }
                        continue;
                    }
                    if (ir instanceof LIVIR liVIR) {
                        if (!usedRegs.contains(liVIR.getTarget())) {
                            block.remove(irId);
                            modified = true;
                            irId--;
                        }
                        continue;
                    }
                    if (ir instanceof LoadVIR loadVIR) {
                        if (!usedRegs.contains(loadVIR.getTarget())) {
                            block.remove(irId);
                            modified = true;
                            irId--;
                        }
                        continue;
                    }
                    if (ir instanceof MovVIR movVIR) {
                        if (!usedRegs.contains(movVIR.getTarget())) {
                            block.remove(irId);
                            modified = true;
                            irId--;
                        }
                        continue;
                    }
                    if (ir instanceof StoreVIR storeVIR) {
                        if (!usedSymbols.contains(storeVIR.getSymbol())) {
                            block.remove(irId);
                            modified = true;
                            irId--;
                        }
                        continue;
                    }
                    if (ir instanceof UnaryVIR unaryVIR) {
                        if (!usedRegs.contains(unaryVIR.getResult())) {
                            block.remove(irId);
                            modified = true;
                            irId--;
                        }
                        continue;
                    }
                }
            }
        }
        return modified;
    }

    private void functionInline() {
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
                        if (ir instanceof CallVIR callVIR && funcCallRelations.containsKey(callVIR.getFunc()))
                            calledFuncs.add(callVIR.getFunc());
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
                        Map<VReg, VReg> paramRegCopyMap = new HashMap<>();
                        Map<LocalSymbol, LocalSymbol> localCopyMap = new HashMap<>();
                        Block preCallBlock = new Block();
                        Block lastBlock = new Block();
                        for (int i = 0; i < toReplaceCall.getParams().size(); i++) {
                            ParamSymbol param = toReplaceCall.getFunc().getParams().get(i);
                            if (toReplaceCall.getParams().get(i) instanceof VReg reg) {
                                VReg newReg = new VReg(reg.getType(), reg.getSize());
                                paramToRegMap.put(param, newReg);
                                preCallBlock.add(new MovVIR(newReg, reg));
                                paramRegCopyMap.put(newReg, reg);
                                continue;
                            }
                            if (toReplaceCall.getParams().get(i) instanceof Value value) {
                                VReg reg = new VReg(toReplaceCall.getFunc().getParams().get(i).getType(), 4);
                                paramToRegMap.put(param, reg);
                                if (value.getType() == Type.FLOAT)
                                    preCallBlock.add(new LIVIR(reg, value.getFloat()));
                                else
                                    preCallBlock.add(new LIVIR(reg, value.getInt()));
                                continue;
                            }
                            throw new RuntimeException();
                        }
                        for (LocalSymbol local : toInlineFunc.getLocals()) {
                            LocalSymbol newLocal = local.clone();
                            localCopyMap.put(local, newLocal);
                            func.addLocal(newLocal);
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
                            if (oldBlock.getDefaultBlock() == null)
                                newBlock.setDefaultBlock(lastBlock);
                            else
                                newBlock.setDefaultBlock(oldToNewMap.get(oldBlock.getDefaultBlock()));
                            for (Pair<Block.Cond, Block> oldCondBlockEntry : oldBlock.getCondBlocks()) {
                                Block.Cond oldCond = oldCondBlockEntry.first();
                                Block oldCondBlock = oldCondBlockEntry.second();
                                VIRItem left = oldCond.left();
                                if (left instanceof VReg reg) {
                                    if (regMap.containsKey(reg))
                                        left = regMap.get(reg);
                                    else {
                                        VReg newReg = new VReg(reg.getType(), reg.getSize());
                                        left = newReg;
                                        regMap.put(reg, newReg);
                                    }
                                }
                                VIRItem right = oldCond.right();
                                if (right instanceof VReg reg) {
                                    if (regMap.containsKey(reg))
                                        right = regMap.get(reg);
                                    else {
                                        VReg newReg = new VReg(reg.getType(), reg.getSize());
                                        right = newReg;
                                        regMap.put(reg, newReg);
                                    }
                                }
                                Block.Cond newCond = new Block.Cond(oldCond.type(), left, right);
                                Block newCondBlock;
                                if (oldCondBlock == null)
                                    newCondBlock = lastBlock;
                                else
                                    newCondBlock = oldToNewMap.get(oldCondBlock);
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
                                        result = new VReg(result.getType(), result.getSize());
                                        regMap.put(binaryVIR.getResult(), result);
                                    }
                                    VIRItem left = binaryVIR.getLeft();
                                    if (left instanceof VReg reg) {
                                        if (regMap.containsKey(reg))
                                            left = regMap.get(reg);
                                        else {
                                            VReg newReg = new VReg(reg.getType(), reg.getSize());
                                            left = newReg;
                                            regMap.put(reg, newReg);
                                        }
                                    }
                                    VIRItem right = binaryVIR.getRight();
                                    if (right instanceof VReg reg) {
                                        if (regMap.containsKey(reg))
                                            right = regMap.get(reg);
                                        else {
                                            VReg newReg = new VReg(reg.getType(), reg.getSize());
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
                                                VReg newReg = new VReg(reg.getType(), reg.getSize());
                                                params.add(newReg);
                                                regMap.put(reg, newReg);
                                            }
                                        } else
                                            params.add(param);
                                    }
                                    VReg retVal = callVIR.getRetVal();
                                    if (retVal != null) {
                                        if (regMap.containsKey(retVal))
                                            retVal = regMap.get(retVal);
                                        else {
                                            retVal = new VReg(retVal.getType(), retVal.getSize());
                                            regMap.put(callVIR.getRetVal(), retVal);
                                        }
                                    }
                                    newBlock.add(new CallVIR(callVIR.getFunc(), retVal, params));
                                    continue;
                                }
                                if (toReplaceIR instanceof LIVIR liVIR) {
                                    VReg target = liVIR.getTarget();
                                    if (regMap.containsKey(target))
                                        target = regMap.get(target);
                                    else {
                                        target = new VReg(target.getType(), target.getSize());
                                        regMap.put(liVIR.getTarget(), target);
                                    }
                                    newBlock.add(new LIVIR(target, liVIR.second()));
                                    continue;
                                }
                                if (toReplaceIR instanceof LoadVIR loadVIR) {
                                    if (loadVIR.getSymbol() instanceof ParamSymbol paramSymbol) {
                                        VReg toReplaceReg = paramToRegMap.get(paramSymbol);
                                        VReg target = loadVIR.getTarget();
                                        if (regMap.containsKey(target))
                                            target = regMap.get(target);
                                        else {
                                            target = new VReg(target.getType(), target.getSize());
                                            regMap.put(loadVIR.getTarget(), target);
                                        }
                                        if (paramSymbol.isSingle()) {
                                            newBlock.add(new MovVIR(target, toReplaceReg));
                                        } else {
                                            Pair<DataSymbol, List<VIRItem>> toReplaceSymbol =
                                                    arrayParamMap.get(paramRegCopyMap.get(toReplaceReg));
                                            List<VIRItem> dimensions = new ArrayList<>(toReplaceSymbol.second());
                                            for (VIRItem dimension : loadVIR.getDimensions()) {
                                                if (dimension instanceof VReg reg) {
                                                    if (regMap.containsKey(reg))
                                                        dimensions.add(regMap.get(reg));
                                                    else {
                                                        VReg newReg = new VReg(reg.getType(), reg.getSize());
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
                                        target = new VReg(target.getType(), target.getSize());
                                        regMap.put(loadVIR.getTarget(), target);
                                    }
                                    List<VIRItem> dimensions = new ArrayList<>();
                                    for (VIRItem dimension : loadVIR.getDimensions()) {
                                        if (dimension instanceof VReg reg) {
                                            if (regMap.containsKey(reg))
                                                dimensions.add(regMap.get(reg));
                                            else {
                                                VReg newReg = new VReg(reg.getType(), reg.getSize());
                                                dimensions.add(newReg);
                                                regMap.put(reg, newReg);
                                            }
                                        } else
                                            dimensions.add(dimension);
                                    }
                                    DataSymbol symbol = loadVIR.getSymbol();
                                    if (symbol instanceof LocalSymbol local)
                                        symbol = localCopyMap.get(local);
                                    newBlock.add(new LoadVIR(target, dimensions, symbol));
                                    continue;
                                }
                                if (toReplaceIR instanceof MovVIR movVIR) {
                                    VReg target = movVIR.getTarget();
                                    if (regMap.containsKey(target))
                                        target = regMap.get(target);
                                    else {
                                        target = new VReg(target.getType(), target.getSize());
                                        regMap.put(movVIR.getTarget(), target);
                                    }
                                    VReg source = movVIR.getSource();
                                    if (regMap.containsKey(source))
                                        source = regMap.get(source);
                                    else {
                                        source = new VReg(source.getType(), source.getSize());
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
                                            source = new VReg(source.getType(), source.getSize());
                                            regMap.put(storeVIR.getSource(), source);
                                        }
                                        if (paramSymbol.isSingle()) {
                                            newBlock.add(new MovVIR(toReplaceReg, source));
                                        } else {
                                            Pair<DataSymbol, List<VIRItem>> toReplaceSymbol =
                                                    arrayParamMap.get(paramRegCopyMap.get(toReplaceReg));
                                            List<VIRItem> dimensions = new ArrayList<>(toReplaceSymbol.second());
                                            for (VIRItem dimension : storeVIR.getDimensions()) {
                                                if (dimension instanceof VReg reg) {
                                                    if (regMap.containsKey(reg))
                                                        dimensions.add(regMap.get(reg));
                                                    else {
                                                        VReg newReg = new VReg(reg.getType(), reg.getSize());
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
                                    DataSymbol symbol = storeVIR.getSymbol();
                                    if (symbol instanceof LocalSymbol local)
                                        symbol = localCopyMap.get(local);
                                    List<VIRItem> dimensions = new ArrayList<>();
                                    for (VIRItem dimension : storeVIR.getDimensions()) {
                                        if (dimension instanceof VReg reg) {
                                            if (regMap.containsKey(reg))
                                                dimensions.add(regMap.get(reg));
                                            else {
                                                VReg newReg = new VReg(reg.getType(), reg.getSize());
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
                                        source = new VReg(source.getType(), source.getSize());
                                        regMap.put(storeVIR.getSource(), source);
                                    }
                                    newBlock.add(new StoreVIR(symbol, dimensions, source));
                                    continue;
                                }
                                if (toReplaceIR instanceof UnaryVIR unaryVIR) {
                                    VReg result = unaryVIR.getResult();
                                    if (regMap.containsKey(result))
                                        result = regMap.get(result);
                                    else {
                                        result = new VReg(result.getType(), result.getSize());
                                        regMap.put(unaryVIR.getResult(), result);
                                    }
                                    VIRItem source = unaryVIR.getSource();
                                    if (source instanceof VReg reg) {
                                        if (regMap.containsKey(reg))
                                            source = regMap.get(reg);
                                        else {
                                            VReg newReg = new VReg(reg.getType(), reg.getSize());
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

    private boolean mergeBlocks() {
        boolean modified = false;
        for (VirtualFunction func : funcs.values()) {
            List<Block> blocks = func.getBlocks();
            boolean toContinue;
            do {
                toContinue = false;
                Map<Block, Set<Block>> prevBlockMap = new HashMap<>();
                for (Block block : blocks)
                    prevBlockMap.put(block, new HashSet<>());
                for (Block block : blocks) {
                    block.getCondBlocks().stream().map(Pair::second).filter(Objects::nonNull).forEach(nextBlock -> prevBlockMap.get(nextBlock).add(block));
                    Block nextBlock = block.getDefaultBlock();
                    if (nextBlock != null)
                        prevBlockMap.get(nextBlock).add(block);
                }
                Block firstBlock = blocks.get(0);
                if (firstBlock.isEmpty() && firstBlock.getCondBlocks().isEmpty()) {
                    Block nextBlock = firstBlock.getDefaultBlock();
                    int nextIndex = blocks.indexOf(nextBlock);
                    blocks.set(0, nextBlock);
                    blocks.remove(nextIndex);
                    for (Block block : blocks) {
                        List<Pair<Block.Cond, Block>> condBlocks = block.getCondBlocks();
                        for (int i = 0; i < condBlocks.size(); i++) {
                            Pair<Block.Cond, Block> condBlock = condBlocks.get(i);
                            if (condBlock.second() == firstBlock)
                                condBlocks.set(i, new Pair<>(condBlock.first(), nextBlock));
                        }
                        if (block.getDefaultBlock() == firstBlock)
                            block.setDefaultBlock(nextBlock);
                    }
                    modified = true;
                    continue;
                }
                Block curBlock = null, toMergeBlock = null;
                for (Block block : blocks) {
                    Block nextBlock = block.getDefaultBlock();
                    if (nextBlock == null || !block.getCondBlocks().isEmpty() || nextBlock == blocks.get(0))
                        continue;
                    if (prevBlockMap.get(nextBlock).size() == 1 && prevBlockMap.get(nextBlock).iterator().next() == block) {
                        curBlock = block;
                        toMergeBlock = nextBlock;
                        break;
                    }
                }
                if (toMergeBlock != null) {
                    curBlock.addAll(toMergeBlock);
                    curBlock.clearCondBlocks();
                    toMergeBlock.getCondBlocks().forEach(curBlock::setCondBlock);
                    curBlock.setDefaultBlock(toMergeBlock.getDefaultBlock());
                    blocks.remove(toMergeBlock);
                    toContinue = true;
                    modified = true;
                    continue;
                }
                for (Block block : blocks) {
                    if (block.isEmpty() && block.getCondBlocks().isEmpty()) {
                        toMergeBlock = block;
                        break;
                    }
                }
                if (toMergeBlock != null) {
                    Set<Block> prevBlocks = prevBlockMap.get(toMergeBlock);
                    for (Block prevBlock : prevBlocks) {
                        List<Pair<Block.Cond, Block>> condBlocks = prevBlock.getCondBlocks();
                        for (int i = 0; i < condBlocks.size(); i++) {
                            Pair<Block.Cond, Block> condBlock = condBlocks.get(i);
                            if (condBlock.second() == toMergeBlock)
                                condBlocks.set(i, new Pair<>(condBlock.first(), toMergeBlock.getDefaultBlock()));
                        }
                        if (prevBlock.getDefaultBlock() == toMergeBlock)
                            prevBlock.setDefaultBlock(toMergeBlock.getDefaultBlock());
                    }
                    blocks.remove(toMergeBlock);
                    toContinue = true;
                    modified = true;
                }
            } while (toContinue);
        }
        return modified;
    }

    private boolean singleLocal2Reg() {
        boolean modified = false;
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
                            reg = new VReg(local.getType(), 4);
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
                            reg = new VReg(local.getType(), 4);
                            local2Reg.put(local, reg);
                        }
                        MovVIR newIR = new MovVIR(reg, storeVIR.getSource());
                        block.set(i, newIR);
                    }
                }
            }
            func.getLocals().removeAll(locals);
            modified |= !locals.isEmpty();
        }
        return modified;
    }

    private void standardize() {
        for (VirtualFunction func : funcs.values()) {
            List<Block> blocks = func.getBlocks();
            for (Block block : blocks) {
                Map<Block.Cond, Block> newCondMap = new HashMap<>();
                for (Pair<Block.Cond, Block> entry : block.getCondBlocks()) {
                    Block.Cond cond = entry.first();
                    Block targetBlock = entry.second();
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
                                case ABS -> Math.abs(value.getInt());
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
