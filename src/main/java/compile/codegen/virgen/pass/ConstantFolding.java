package compile.codegen.virgen.pass;

import common.Pair;
import compile.codegen.virgen.Block;
import compile.codegen.virgen.VReg;
import compile.codegen.virgen.VirtualFunction;
import compile.codegen.virgen.vir.*;
import compile.symbol.GlobalSymbol;
import compile.symbol.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConstantFolding extends Pass {
    public ConstantFolding(Set<GlobalSymbol> globals, Map<String, VirtualFunction> funcs) {
        super(globals, funcs);
    }

    @Override
    public boolean run() {
        boolean modified = false;
        for (VirtualFunction func : funcs.values()) {
            for (Block block : func.getBlocks()) {
                for (int i = 0; i < block.size(); i++) {
                    VIR ir = block.get(i);
                    if (ir instanceof BinaryVIR binaryVIR) {
                        if (binaryVIR.left() instanceof Value left && binaryVIR.right() instanceof Value right) {
                            Value result = switch (binaryVIR.type()) {
                                case ADD -> left.add(right);
                                case SUB -> left.sub(right);
                                case MUL -> left.mul(right);
                                case DIV -> left.div(right);
                                case MOD -> left.mod(right);
                                case EQ -> left.eq(right);
                                case NE -> left.ne(right);
                                case GE -> left.ge(right);
                                case GT -> left.gt(right);
                                case LE -> left.le(right);
                                case LT -> left.lt(right);
                            };
                            switch (result.getType()) {
                                case INT -> block.set(i, new LiVIR(binaryVIR.target(), result.getInt()));
                                case FLOAT -> block.set(i, new LiVIR(binaryVIR.target(), result.getFloat()));
                            }
                            modified = true;
                        }
                        continue;
                    }
                    if (ir instanceof UnaryVIR unaryVIR) {
                        if (unaryVIR.source() instanceof Value source) {
                            Value result = switch (unaryVIR.type()) {
                                case F2I -> source.toInt();
                                case I2F -> source.toFloat();
                                case L_NOT -> source.lNot();
                                case NEG -> source.neg();
                                case ABS -> source.abs();
                            };
                            switch (result.getType()) {
                                case INT -> block.set(i, new LiVIR(unaryVIR.target(), result.getInt()));
                                case FLOAT -> block.set(i, new LiVIR(unaryVIR.target(), result.getFloat()));
                            }
                            modified = true;
                        }
                        continue;
                    }
                }
                List<Pair<Block.Cond, Block>> condBlocks = block.getCondBlocks();
                for (int i = 0; i < condBlocks.size(); i++) {
                    Pair<Block.Cond, Block> condBlock = condBlocks.get(i);
                    Block.Cond cond = condBlock.first();
                    if (cond.left() instanceof Value left && cond.right() instanceof Value right) {
                        Value result = switch (cond.type()) {
                            case EQ -> left.eq(right);
                            case NE -> left.ne(right);
                            case GE -> left.ge(right);
                            case GT -> left.gt(right);
                            case LE -> left.le(right);
                            case LT -> left.lt(right);
                        };
                        if (result.isZero()) {
                            condBlocks.remove(i);
                            i--;
                            modified = true;
                            continue;
                        }
                        block.setDefaultBlock(condBlock.second());
                        while (condBlocks.size() > i)
                            condBlocks.remove(condBlocks.size() - 1);
                        modified = true;
                        break;
                    }
                }
            }
        }
        return modified;
    }

    private Map<VReg, Value> analyzeConst(VirtualFunction func) {
        Map<VReg, Value> constMap = new HashMap<>();
        for (Block block : func.getBlocks())
            for (VIR ir : block)
                if (ir instanceof LiVIR liVIR) {
                    if (liVIR.value() instanceof Float value)
                        constMap.put(liVIR.target(), new Value(value));
                    if (liVIR.value() instanceof Integer value)
                        constMap.put(liVIR.target(), new Value(value));
                }
        return constMap;
    }

    private boolean replaceIRs(VirtualFunction func, Map<VReg, Value> constMap) {
        boolean modified = false;
        for (Block block : func.getBlocks()) {
            for (int i = 0; i < block.size(); i++) {
                VIR ir = block.get(i);
                if (ir instanceof BinaryVIR binaryVIR) {
                    VIRItem newLeft = binaryVIR.left();
                    if (newLeft instanceof VReg reg && constMap.containsKey(reg)) {
                        newLeft = constMap.get(reg);
                        modified = true;
                    }
                    VIRItem newRight = binaryVIR.right();
                    if (newRight instanceof VReg reg && constMap.containsKey(reg)) {
                        newRight = constMap.get(reg);
                        modified = true;
                    }
                    block.set(i, new BinaryVIR(binaryVIR.type(), binaryVIR.target(), newLeft, newRight));
                    continue;
                }
                if (ir instanceof CallVIR callVIR) {
                    List<VIRItem> newParams = callVIR.params();
                    for (int j = 0; j < newParams.size(); j++) {
                        VIRItem newParam = newParams.get(j);
                        if (newParam instanceof VReg reg && constMap.containsKey(reg)) {
                            newParam = constMap.get(reg);
                            modified = true;
                        }
                        newParams.set(j, newParam);
                    }
                    block.set(i, new CallVIR(callVIR.func(), callVIR.target(), newParams));
                    continue;
                }
                if (ir instanceof LoadVIR loadVIR) {
                    List<VIRItem> newIndexes = loadVIR.indexes();
                    for (int j = 0; j < newIndexes.size(); j++) {
                        VIRItem newIndex = newIndexes.get(j);
                        if (newIndex instanceof VReg reg && constMap.containsKey(reg)) {
                            newIndex = constMap.get(reg);
                            modified = true;
                        }
                        newIndexes.set(j, newIndex);
                    }
                    block.set(i, new LoadVIR(loadVIR.target(), loadVIR.symbol(), newIndexes));
                    continue;
                }
                if (ir instanceof StoreVIR storeVIR) {
                    List<VIRItem> newIndexes = storeVIR.indexes();
                    for (int j = 0; j < newIndexes.size(); j++) {
                        VIRItem newIndex = newIndexes.get(j);
                        if (newIndex instanceof VReg reg && constMap.containsKey(reg)) {
                            newIndex = constMap.get(reg);
                            modified = true;
                        }
                        newIndexes.set(j, newIndex);
                    }
                    block.set(i, new StoreVIR(storeVIR.symbol(), newIndexes, storeVIR.source()));
                    continue;
                }
                if (ir instanceof UnaryVIR unaryVIR) {
                    VIRItem newSource = unaryVIR.source();
                    if (newSource instanceof VReg reg && constMap.containsKey(reg)) {
                        newSource = constMap.get(reg);
                        modified = true;
                    }
                    block.set(i, new UnaryVIR(unaryVIR.type(), unaryVIR.target(), newSource));
                    continue;
                }
            }
            List<Pair<Block.Cond, Block>> condBlocks = block.getCondBlocks();
            for (int i = 0; i < condBlocks.size(); i++) {
                Pair<Block.Cond, Block> condBlock = condBlocks.get(i);
                Block.Cond cond = condBlock.first();
                VIRItem newLeft = cond.left();
                if (newLeft instanceof VReg reg && constMap.containsKey(reg)) {
                    newLeft = constMap.get(reg);
                    modified = true;
                }
                VIRItem newRight = cond.right();
                if (newRight instanceof VReg reg && constMap.containsKey(reg)) {
                    newRight = constMap.get(reg);
                    modified = true;
                }
                condBlocks.set(i, new Pair<>(new Block.Cond(cond.type(), newLeft, newRight), condBlock.second()));
            }
        }
        return modified;
    }
}
