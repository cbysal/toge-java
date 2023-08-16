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

public class ConstantPropagation extends Pass {
    public ConstantPropagation(Set<GlobalSymbol> globals, Map<String, VirtualFunction> funcs) {
        super(globals, funcs);
    }

    @Override
    public boolean run() {
        boolean modified = false;
        for (VirtualFunction func : funcs.values()) {
            Map<VReg, Value> constMap = analyzeConst(func);
            modified |= replaceIRs(func, constMap);
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
                if (ir instanceof BranchVIR branchVIR) {
                    VIRItem newLeft = branchVIR.left();
                    if (newLeft instanceof VReg reg && constMap.containsKey(reg)) {
                        newLeft = constMap.get(reg);
                        modified = true;
                    }
                    VIRItem newRight = branchVIR.right();
                    if (newRight instanceof VReg reg && constMap.containsKey(reg)) {
                        newRight = constMap.get(reg);
                        modified = true;
                    }
                    block.set(i, new BranchVIR(branchVIR.type(), newLeft, newRight, branchVIR.trueBlock(),
                            branchVIR.falseBlock()));
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
                if (ir instanceof MovVIR movVIR) {
                    VReg source = movVIR.source();
                    if (constMap.containsKey(source)) {
                        Value newSource = constMap.get(source);
                        switch (newSource.getType()) {
                            case INT -> block.set(i, new LiVIR(movVIR.target(), newSource.getInt()));
                            case FLOAT -> block.set(i, new LiVIR(movVIR.target(), newSource.getFloat()));
                        }
                        modified = true;
                    }
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
        }
        return modified;
    }
}
