package compile.codegen.virgen.pass;

import compile.codegen.virgen.Block;
import compile.codegen.virgen.VReg;
import compile.codegen.virgen.VirtualFunction;
import compile.codegen.virgen.vir.*;
import compile.symbol.GlobalSymbol;
import compile.symbol.Type;
import compile.symbol.Value;

import java.util.*;

public class AssignmentPropagation extends Pass {
    public AssignmentPropagation(Set<GlobalSymbol> globals, Map<String, VirtualFunction> funcs) {
        super(globals, funcs);
    }

    @Override
    public boolean run() {
        boolean modified = false;
        for (VirtualFunction func : funcs.values()) {
            boolean toContinue;
            do {
                toContinue = false;
                List<Block> blocks = func.getBlocks();
                for (Block block : blocks) {
                    BiHashMap<VReg, VReg> regToRegMap = new BiHashMap<>();
                    for (int irId = 0; irId < block.size(); irId++) {
                        VIR ir = block.get(irId);
                        if (ir instanceof BinaryVIR binaryVIR) {
                            VIRItem left = binaryVIR.left;
                            VIRItem right = binaryVIR.right;
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
                            block.set(irId, new BinaryVIR(binaryVIR.type, binaryVIR.target, left, right));
                            regToRegMap.remove(binaryVIR.target);
                            regToRegMap.removeByValue(binaryVIR.target);
                            continue;
                        }
                        if (ir instanceof BranchVIR branchVIR) {
                            VIRItem left = branchVIR.left;
                            VIRItem right = branchVIR.right;
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
                            block.set(irId, new BranchVIR(branchVIR.type, left, right, branchVIR.trueBlock,
                                    branchVIR.falseBlock));
                            continue;
                        }
                        if (ir instanceof CallVIR callVIR) {
                            List<VIRItem> params = callVIR.params;
                            for (int j = 0; j < params.size(); j++)
                                if (params.get(j) instanceof VReg reg && regToRegMap.containsKey(reg)) {
                                    params.set(j, regToRegMap.get(reg));
                                    toContinue = true;
                                    modified = true;
                                }
                            regToRegMap.remove(callVIR.target);
                            regToRegMap.removeByValue(callVIR.target);
                            continue;
                        }
                        if (ir instanceof LiVIR liVIR) {
                            regToRegMap.remove(liVIR.target);
                            regToRegMap.removeByValue(liVIR.target);
                            continue;
                        }
                        if (ir instanceof LoadVIR loadVIR) {
                            List<VIRItem> indexes = loadVIR.indexes;
                            for (int j = 0; j < indexes.size(); j++)
                                if (indexes.get(j) instanceof VReg reg && regToRegMap.containsKey(reg)) {
                                    indexes.set(j, regToRegMap.get(reg));
                                    toContinue = true;
                                    modified = true;
                                }
                            regToRegMap.remove(loadVIR.target);
                            regToRegMap.removeByValue(loadVIR.target);
                            continue;
                        }
                        if (ir instanceof MovVIR movVIR) {
                            if (regToRegMap.containsKey(movVIR.source)) {
                                regToRegMap.put(movVIR.target, regToRegMap.get(movVIR.source));
                                block.set(irId, new MovVIR(movVIR.target, regToRegMap.get(movVIR.source)));
                                toContinue = true;
                                modified = true;
                            } else {
                                regToRegMap.put(movVIR.target, movVIR.source);
                            }
                            regToRegMap.removeByValue(movVIR.target);
                            continue;
                        }
                        if (ir instanceof StoreVIR storeVIR) {
                            List<VIRItem> indexes = storeVIR.indexes;
                            for (int j = 0; j < indexes.size(); j++)
                                if (indexes.get(j) instanceof VReg reg && regToRegMap.containsKey(reg)) {
                                    indexes.set(j, regToRegMap.get(reg));
                                    toContinue = true;
                                    modified = true;
                                }
                            if (regToRegMap.containsKey(storeVIR.source)) {
                                block.set(irId, new StoreVIR(storeVIR.symbol, indexes,
                                        regToRegMap.get(storeVIR.source)));
                                toContinue = true;
                                modified = true;
                            }
                            continue;
                        }
                        if (ir instanceof UnaryVIR unaryVIR) {
                            if (unaryVIR.source instanceof VReg reg && regToRegMap.containsKey(reg)) {
                                block.set(irId, new UnaryVIR(unaryVIR.type, unaryVIR.target, regToRegMap.get(reg)));
                                toContinue = true;
                                modified = true;
                            }
                            regToRegMap.remove(unaryVIR.target);
                            regToRegMap.removeByValue(unaryVIR.target);
                            continue;
                        }
                    }
                }
                standardize(func);
            } while (toContinue);
        }
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
                        block.set(irId, new LiVIR(binaryVIR.target, result.floatValue()));
                    else
                        block.set(irId, new LiVIR(binaryVIR.target, result.intValue()));
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
                        block.set(irId, new LiVIR(unaryVIR.target, result.floatValue()));
                    else
                        block.set(irId, new LiVIR(unaryVIR.target, result.intValue()));
                    continue;
                }
            }
        }
    }

    private static class BiHashMap<K, V> {
        private final Map<K, V> map = new HashMap<>();
        private final Map<V, Set<K>> iMap = new HashMap<>();

        public void put(K key, V value) {
            if (map.containsKey(key)) {
                iMap.get(map.get(key)).remove(key);
                map.remove(key);
            }
            map.put(key, value);
            if (!iMap.containsKey(value))
                iMap.put(value, new HashSet<>());
            iMap.get(value).add(key);
        }

        public V get(K key) {
            return map.get(key);
        }

        public boolean containsKey(K key) {
            return map.containsKey(key);
        }

        public void remove(K key) {
            if (map.containsKey(key)) {
                iMap.get(map.get(key)).remove(key);
                map.remove(key);
            }
        }

        public void removeByValue(V value) {
            if (iMap.containsKey(value))
                for (K key : iMap.get(value))
                    map.remove(key);
            iMap.remove(value);
        }
    }
}
