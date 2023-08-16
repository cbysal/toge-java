package compile.codegen.virgen.pass;

import common.Pair;
import compile.codegen.virgen.Block;
import compile.codegen.virgen.VReg;
import compile.codegen.virgen.VirtualFunction;
import compile.codegen.virgen.vir.*;
import compile.symbol.GlobalSymbol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AssignmentPropagation extends Pass {
    public AssignmentPropagation(Set<GlobalSymbol> globals, Map<String, VirtualFunction> funcs) {
        super(globals, funcs);
    }

    @Override
    public boolean run() {
        boolean modified = false;
        for (VirtualFunction func : funcs.values()) {
            Map<VReg, Pair<VReg, Block>> assignMap = analyzeAssignment(func);
            modified |= replaceIRs(func, assignMap);
        }
        return modified;
    }

    private Map<VReg, Pair<VReg, Block>> analyzeAssignment(VirtualFunction func) {
        Map<VReg, Block> regToBlockMap = new HashMap<>();
        for (Block block : func.getBlocks())
            for (VIR ir : block)
                if (ir.getWrite() != null)
                    regToBlockMap.put(ir.getWrite(), block);
        Map<VReg, Pair<VReg, Block>> assignMap = new HashMap<>();
        for (Block block : func.getBlocks())
            for (VIR ir : block)
                if (ir instanceof MovVIR movVIR)
                    assignMap.put(movVIR.target(), new Pair<>(movVIR.source(), regToBlockMap.get(movVIR.source())));
        for (Map.Entry<VReg, Pair<VReg, Block>> entry : assignMap.entrySet()) {
            Pair<VReg, Block> regWithBlock = entry.getValue();
            while (assignMap.containsKey(regWithBlock.first())) {
                regWithBlock = assignMap.get(regWithBlock.first());
                entry.setValue(regWithBlock);
            }
        }
        return assignMap;
    }

    private boolean replaceIRs(VirtualFunction func, Map<VReg, Pair<VReg, Block>> assignMap) {
        boolean modified = false;
        for (Block block : func.getBlocks()) {
            for (int i = 0; i < block.size(); i++) {
                VIR ir = block.get(i);
                if (ir instanceof BinaryVIR binaryVIR) {
                    VIRItem newLeft = binaryVIR.left();
                    if (newLeft instanceof VReg reg && assignMap.containsKey(reg)) {
                        newLeft = assignMap.get(reg).first();
                        modified = true;
                    }
                    VIRItem newRight = binaryVIR.right();
                    if (newRight instanceof VReg reg && assignMap.containsKey(reg)) {
                        newRight = assignMap.get(reg).first();
                        modified = true;
                    }
                    block.set(i, new BinaryVIR(binaryVIR.type(), binaryVIR.target(), newLeft, newRight));
                    continue;
                }
                if (ir instanceof BranchVIR branchVIR) {
                    VIRItem newLeft = branchVIR.left();
                    if (newLeft instanceof VReg reg && assignMap.containsKey(reg)) {
                        newLeft = assignMap.get(reg).first();
                        modified = true;
                    }
                    VIRItem newRight = branchVIR.right();
                    if (newRight instanceof VReg reg && assignMap.containsKey(reg)) {
                        newRight = assignMap.get(reg).first();
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
                        if (newParam instanceof VReg reg && assignMap.containsKey(reg)) {
                            newParam = assignMap.get(reg).first();
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
                        if (newIndex instanceof VReg reg && assignMap.containsKey(reg)) {
                            newIndex = assignMap.get(reg).first();
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
                        if (newIndex instanceof VReg reg && assignMap.containsKey(reg)) {
                            newIndex = assignMap.get(reg).first();
                            modified = true;
                        }
                        newIndexes.set(j, newIndex);
                    }
                    VReg newSource = storeVIR.source();
                    if (assignMap.containsKey(newSource)) {
                        newSource = assignMap.get(newSource).first();
                        modified = true;
                    }
                    block.set(i, new StoreVIR(storeVIR.symbol(), newIndexes, newSource));
                    continue;
                }
                if (ir instanceof UnaryVIR unaryVIR) {
                    VIRItem newSource = unaryVIR.source();
                    if (newSource instanceof VReg reg && assignMap.containsKey(reg)) {
                        newSource = assignMap.get(reg).first();
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
