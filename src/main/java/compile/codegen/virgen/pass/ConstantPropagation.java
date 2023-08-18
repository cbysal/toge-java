package compile.codegen.virgen.pass;

import compile.codegen.virgen.Block;
import compile.codegen.virgen.VReg;
import compile.codegen.virgen.VirtualFunction;
import compile.codegen.virgen.vir.*;
import compile.symbol.GlobalSymbol;
import compile.symbol.Type;
import compile.symbol.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConstantPropagation extends Pass {
    public ConstantPropagation(Set<GlobalSymbol> globals, Map<String, VirtualFunction> funcs) {
        super(globals, funcs);
    }

    @Override
    public boolean run() {
        boolean modified = false;
        for (VirtualFunction func : funcs.values()) {
            modified |= constPassForBlock(func);
            modified |= constPassForFunc(func);
        }
        return modified;
    }

    private boolean constPassForBlock(VirtualFunction func) {
        boolean modified = false;
        boolean toContinue;
        do {
            toContinue = false;
            List<Block> blocks = func.getBlocks();
            for (Block block : blocks) {
                Map<GlobalSymbol, VReg> globalToRegMap = new HashMap<>();
                Map<VReg, Number> regToValueMap = new HashMap<>();
                for (int irId = 0; irId < block.size(); irId++) {
                    VIR ir = block.get(irId);
                    if (ir instanceof BinaryVIR binaryVIR) {
                        VIRItem left = binaryVIR.left;
                        VIRItem right = binaryVIR.right;
                        if (left instanceof VReg reg && regToValueMap.containsKey(reg)) {
                            left = reg.getType() == Type.FLOAT ? new Value(regToValueMap.get(reg).floatValue()) :
                                    new Value(regToValueMap.get(reg).intValue());
                            toContinue = true;
                            modified = true;
                        }
                        if (right instanceof VReg reg && regToValueMap.containsKey(reg)) {
                            right = reg.getType() == Type.FLOAT ? new Value(regToValueMap.get(reg).floatValue()) :
                                    new Value(regToValueMap.get(reg).intValue());
                            toContinue = true;
                            modified = true;
                        }
                        block.set(irId, new BinaryVIR(binaryVIR.type, binaryVIR.target, left, right));
                        regToValueMap.remove(binaryVIR.target);
                        globalToRegMap.entrySet().removeIf(entry -> entry.getValue() == binaryVIR.target);
                        continue;
                    }
                    if (ir instanceof BranchVIR branchVIR) {
                        VIRItem left = branchVIR.left;
                        VIRItem right = branchVIR.right;
                        if (left instanceof VReg reg && regToValueMap.containsKey(reg)) {
                            left = reg.getType() == Type.FLOAT ? new Value(regToValueMap.get(reg).floatValue()) :
                                    new Value(regToValueMap.get(reg).intValue());
                            toContinue = true;
                            modified = true;
                        }
                        if (right instanceof VReg reg && regToValueMap.containsKey(reg)) {
                            right = reg.getType() == Type.FLOAT ? new Value(regToValueMap.get(reg).floatValue()) :
                                    new Value(regToValueMap.get(reg).intValue());
                            toContinue = true;
                            modified = true;
                        }
                        block.set(irId, new BranchVIR(branchVIR.type, left, right, branchVIR.trueBlock,
                                branchVIR.falseBlock));
                        continue;
                    }
                    if (ir instanceof CallVIR callVIR) {
                        List<VIRItem> params = callVIR.params;
                        for (int j = 0; j < params.size(); j++)
                            if (params.get(j) instanceof VReg reg && regToValueMap.containsKey(reg)) {
                                params.set(j, reg.getType() == Type.FLOAT ?
                                        new Value(regToValueMap.get(reg).floatValue()) :
                                        new Value(regToValueMap.get(reg).intValue()));
                                toContinue = true;
                                modified = true;
                            }
                        if (callVIR.target != null) {
                            regToValueMap.remove(callVIR.target);
                            globalToRegMap.entrySet().removeIf(entry -> entry.getValue() == callVIR.target);
                        }
                        continue;
                    }
                    if (ir instanceof LiVIR liVIR) {
                        regToValueMap.put(liVIR.target, liVIR.value);
                        globalToRegMap.entrySet().removeIf(entry -> entry.getValue() == liVIR.target);
                        continue;
                    }
                    if (ir instanceof LoadVIR loadVIR) {
                        List<VIRItem> indexes = loadVIR.indexes;
                        for (int j = 0; j < indexes.size(); j++)
                            if (indexes.get(j) instanceof VReg reg && regToValueMap.containsKey(reg)) {
                                indexes.set(j, reg.getType() == Type.FLOAT ?
                                        new Value(regToValueMap.get(reg).floatValue()) :
                                        new Value(regToValueMap.get(reg).intValue()));
                                toContinue = true;
                                modified = true;
                            }
                        if (loadVIR.symbol instanceof GlobalSymbol global && globalToRegMap.containsKey(global))
                            block.set(irId, new MovVIR(loadVIR.target, globalToRegMap.get(global)));
                        regToValueMap.remove(loadVIR.target);
                        globalToRegMap.entrySet().removeIf(entry -> entry.getValue() == loadVIR.target);
                        continue;
                    }
                    if (ir instanceof MovVIR movVIR) {
                        if (regToValueMap.containsKey(movVIR.source)) {
                            block.set(irId, new LiVIR(movVIR.target, regToValueMap.get(movVIR.source)));
                            toContinue = true;
                            modified = true;
                        }
                        regToValueMap.remove(movVIR.target);
                        globalToRegMap.entrySet().removeIf(entry -> entry.getValue() == movVIR.target);
                        continue;
                    }
                    if (ir instanceof StoreVIR storeVIR) {
                        List<VIRItem> indexes = storeVIR.indexes;
                        for (int j = 0; j < indexes.size(); j++)
                            if (indexes.get(j) instanceof VReg reg && regToValueMap.containsKey(reg)) {
                                indexes.set(j, reg.getType() == Type.FLOAT ?
                                        new Value(regToValueMap.get(reg).floatValue()) :
                                        new Value(regToValueMap.get(reg).intValue()));
                                toContinue = true;
                                modified = true;
                            }
                        if (storeVIR.symbol instanceof GlobalSymbol global && global.isSingle())
                            globalToRegMap.put(global, storeVIR.source);
                        continue;
                    }
                    if (ir instanceof UnaryVIR unaryVIR) {
                        if (unaryVIR.source instanceof VReg reg && regToValueMap.containsKey(reg)) {
                            block.set(irId, new UnaryVIR(unaryVIR.type, unaryVIR.target, reg.getType() == Type.FLOAT
                                    ? new Value(regToValueMap.get(reg).floatValue()) :
                                    new Value(regToValueMap.get(reg).intValue())));
                            toContinue = true;
                            modified = true;
                        }
                        regToValueMap.remove(unaryVIR.target);
                        globalToRegMap.entrySet().removeIf(entry -> entry.getValue() == unaryVIR.target);
                        continue;
                    }
                }
            }
            standardize(func);
        } while (toContinue);
        return modified;
    }

    private boolean constPassForFunc(VirtualFunction func) {
        boolean modified = false;
        boolean toContinue;
        do {
            toContinue = false;
            List<Block> blocks = func.getBlocks();
            Map<VReg, Integer> writeCounter = new HashMap<>();
            Map<VReg, Number> regToValueMap = new HashMap<>();
            for (Block block : blocks) {
                for (VIR ir : block) {
                    if (ir instanceof BinaryVIR binaryVIR) {
                        writeCounter.put(binaryVIR.target, writeCounter.getOrDefault(binaryVIR.target, 0) + 1);
                        continue;
                    }
                    if (ir instanceof CallVIR callVIR) {
                        if (callVIR.target != null)
                            writeCounter.put(callVIR.target, writeCounter.getOrDefault(callVIR.target, 0) + 1);
                        continue;
                    }
                    if (ir instanceof LiVIR liVIR) {
                        writeCounter.put(liVIR.target, writeCounter.getOrDefault(liVIR.target, 0) + 1);
                        regToValueMap.put(liVIR.target, liVIR.value);
                        continue;
                    }
                    if (ir instanceof LoadVIR loadVIR) {
                        writeCounter.put(loadVIR.target, writeCounter.getOrDefault(loadVIR.target, 0) + 1);
                        continue;
                    }
                    if (ir instanceof MovVIR movVIR) {
                        writeCounter.put(movVIR.target, writeCounter.getOrDefault(movVIR.target, 0) + 1);
                        continue;
                    }
                    if (ir instanceof UnaryVIR unaryVIR) {
                        writeCounter.put(unaryVIR.target, writeCounter.getOrDefault(unaryVIR.target, 0) + 1);
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
                        VIRItem left = binaryVIR.left;
                        VIRItem right = binaryVIR.right;
                        if (left instanceof VReg reg && regToValueMap.containsKey(reg)) {
                            left = reg.getType() == Type.FLOAT ? new Value(regToValueMap.get(reg).floatValue()) :
                                    new Value(regToValueMap.get(reg).intValue());
                            toContinue = true;
                            modified = true;
                        }
                        if (right instanceof VReg reg && regToValueMap.containsKey(reg)) {
                            right = reg.getType() == Type.FLOAT ? new Value(regToValueMap.get(reg).floatValue()) :
                                    new Value(regToValueMap.get(reg).intValue());
                            toContinue = true;
                            modified = true;
                        }
                        block.set(irId, new BinaryVIR(binaryVIR.type, binaryVIR.target, left, right));
                        continue;
                    }
                    if (ir instanceof BranchVIR branchVIR) {
                        VIRItem left = branchVIR.left;
                        VIRItem right = branchVIR.right;
                        if (left instanceof VReg reg && regToValueMap.containsKey(reg)) {
                            left = reg.getType() == Type.FLOAT ? new Value(regToValueMap.get(reg).floatValue()) :
                                    new Value(regToValueMap.get(reg).intValue());
                            toContinue = true;
                            modified = true;
                        }
                        if (right instanceof VReg reg && regToValueMap.containsKey(reg)) {
                            right = reg.getType() == Type.FLOAT ? new Value(regToValueMap.get(reg).floatValue()) :
                                    new Value(regToValueMap.get(reg).intValue());
                            toContinue = true;
                            modified = true;
                        }
                        block.set(irId, new BranchVIR(branchVIR.type, left, right, branchVIR.trueBlock,
                                branchVIR.falseBlock));
                        continue;
                    }
                    if (ir instanceof CallVIR callVIR) {
                        List<VIRItem> params = callVIR.params;
                        for (int j = 0; j < params.size(); j++)
                            if (params.get(j) instanceof VReg reg && regToValueMap.containsKey(reg)) {
                                params.set(j, reg.getType() == Type.FLOAT ?
                                        new Value(regToValueMap.get(reg).floatValue()) :
                                        new Value(regToValueMap.get(reg).intValue()));
                                toContinue = true;
                                modified = true;
                            }
                        continue;
                    }
                    if (ir instanceof LoadVIR loadVIR) {
                        List<VIRItem> indexes = loadVIR.indexes;
                        for (int j = 0; j < indexes.size(); j++)
                            if (indexes.get(j) instanceof VReg reg && regToValueMap.containsKey(reg)) {
                                indexes.set(j, reg.getType() == Type.FLOAT ?
                                        new Value(regToValueMap.get(reg).floatValue()) :
                                        new Value(regToValueMap.get(reg).intValue()));
                                toContinue = true;
                                modified = true;
                            }
                        continue;
                    }
                    if (ir instanceof MovVIR movVIR) {
                        if (regToValueMap.containsKey(movVIR.source)) {
                            block.set(irId, new LiVIR(movVIR.target, regToValueMap.get(movVIR.source)));
                            toContinue = true;
                            modified = true;
                        }
                        continue;
                    }
                    if (ir instanceof StoreVIR storeVIR) {
                        List<VIRItem> indexes = storeVIR.indexes;
                        for (int j = 0; j < indexes.size(); j++)
                            if (indexes.get(j) instanceof VReg reg && regToValueMap.containsKey(reg)) {
                                indexes.set(j, reg.getType() == Type.FLOAT ?
                                        new Value(regToValueMap.get(reg).floatValue()) :
                                        new Value(regToValueMap.get(reg).intValue()));
                                toContinue = true;
                                modified = true;
                            }
                        continue;
                    }
                    if (ir instanceof UnaryVIR unaryVIR) {
                        if (unaryVIR.source instanceof VReg reg && regToValueMap.containsKey(reg)) {
                            block.set(irId, new UnaryVIR(unaryVIR.type, unaryVIR.target, reg.getType() == Type.FLOAT
                                    ? new Value(regToValueMap.get(reg).floatValue()) :
                                    new Value(regToValueMap.get(reg).intValue())));
                            toContinue = true;
                            modified = true;
                        }
                        continue;
                    }
                }
            }
            standardize(func);
        } while (toContinue);
        return modified;
    }

    private void standardize(VirtualFunction func) {
        List<Block> blocks = func.getBlocks();
        for (Block block : blocks) {
            for (int irId = 0; irId < block.size(); irId++) {
                VIR ir = block.get(irId);
                if (ir instanceof BinaryVIR binaryVIR && binaryVIR.left instanceof Value value1 && binaryVIR.right instanceof Value value2) {
                    Value result = switch (binaryVIR.type) {
                        case ADD -> value1.add(value2);
                        case SUB -> value1.sub(value2);
                        case MUL -> value1.mul(value2);
                        case DIV -> value1.div(value2);
                        case MOD -> value1.mod(value2);
                        case EQ -> value1.eq(value2);
                        case NE -> value1.ne(value2);
                        case GE -> value1.ge(value2);
                        case GT -> value1.gt(value2);
                        case LE -> value1.le(value2);
                        case LT -> value1.lt(value2);
                    };
                    if (binaryVIR.target.getType() == Type.FLOAT)
                        block.set(irId, new LiVIR(binaryVIR.target, result.getFloat()));
                    else
                        block.set(irId, new LiVIR(binaryVIR.target, result.getInt()));
                    continue;
                }
                if (ir instanceof BranchVIR branchVIR && branchVIR.left instanceof Value value1 && branchVIR.right instanceof Value value2) {
                    Value result = switch (branchVIR.type) {
                        case EQ -> value1.eq(value2);
                        case NE -> value1.ne(value2);
                        case GE -> value1.ge(value2);
                        case GT -> value1.gt(value2);
                        case LE -> value1.le(value2);
                        case LT -> value1.lt(value2);
                    };
                    block.set(irId, new JumpVIR(result.isZero() ? branchVIR.falseBlock : branchVIR.trueBlock));
                    continue;
                }
                if (ir instanceof UnaryVIR unaryVIR && unaryVIR.source instanceof Value value) {
                    Value result = switch (unaryVIR.type) {
                        case F2I -> value.toInt();
                        case I2F -> value.toFloat();
                        case NEG -> value.neg();
                        case L_NOT -> value.lNot();
                        case ABS -> value.abs();
                    };
                    if (unaryVIR.target.getType() == Type.FLOAT)
                        block.set(irId, new LiVIR(unaryVIR.target, result.getFloat()));
                    else
                        block.set(irId, new LiVIR(unaryVIR.target, result.getInt()));
                    continue;
                }
            }
        }
    }
}
