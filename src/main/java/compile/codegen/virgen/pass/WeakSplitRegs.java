package compile.codegen.virgen.pass;

import compile.codegen.virgen.Block;
import compile.codegen.virgen.VReg;
import compile.codegen.virgen.VirtualFunction;
import compile.codegen.virgen.vir.*;
import compile.symbol.GlobalSymbol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WeakSplitRegs extends Pass {
    public WeakSplitRegs(Set<GlobalSymbol> globals, Map<String, VirtualFunction> funcs) {
        super(globals, funcs);
    }

    @Override
    public boolean run() {
        boolean modified = false;
        for (VirtualFunction func : funcs.values()) {
            for (Block block : func.getBlocks()) {
                Map<VReg, Integer> lastAssignMap = new HashMap<>();
                for (int i = 0; i < block.size(); i++) {
                    VIR ir = block.get(i);
                    if (ir.getWrite() != null)
                        lastAssignMap.put(ir.getWrite(), i);
                }
                Map<VReg, VReg> assignMap = new HashMap<>();
                for (int i = 0; i < block.size(); i++) {
                    VIR ir = block.get(i);
                    if (ir instanceof BinaryVIR binaryVIR) {
                        VIRItem left = binaryVIR.left;
                        if (left instanceof VReg reg && assignMap.containsKey(reg))
                            left = assignMap.get(reg);
                        VIRItem right = binaryVIR.right;
                        if (right instanceof VReg reg && assignMap.containsKey(reg))
                            right = assignMap.get(reg);
                        VReg target = binaryVIR.target;
                        if (i < lastAssignMap.get(target)) {
                            target = new VReg(target.getType(), target.getSize());
                            assignMap.put(binaryVIR.target, target);
                            modified = true;
                        } else
                            assignMap.remove(target);
                        block.set(i, new BinaryVIR(binaryVIR.type, target, left, right));
                        continue;
                    }
                    if (ir instanceof BranchVIR branchVIR) {
                        VIRItem left = branchVIR.left;
                        if (left instanceof VReg reg && assignMap.containsKey(reg))
                            left = assignMap.get(reg);
                        VIRItem right = branchVIR.right;
                        if (right instanceof VReg reg && assignMap.containsKey(reg))
                            right = assignMap.get(reg);
                        block.set(i, new BranchVIR(branchVIR.type, left, right, branchVIR.trueBlock,
                                branchVIR.falseBlock));
                        continue;
                    }
                    if (ir instanceof CallVIR callVIR) {
                        List<VIRItem> params = callVIR.params;
                        for (int j = 0; j < params.size(); j++) {
                            VIRItem param = params.get(j);
                            if (param instanceof VReg reg && assignMap.containsKey(reg)) {
                                reg = assignMap.get(reg);
                                params.set(j, reg);
                            }
                        }
                        VReg target = callVIR.target;
                        if (target != null) {
                            if (i < lastAssignMap.get(target)) {
                                target = new VReg(target.getType(), target.getSize());
                                assignMap.put(callVIR.target, target);
                                modified = true;
                            } else
                                assignMap.remove(target);
                        }
                        block.set(i, new CallVIR(callVIR.func, target, params));
                        continue;
                    }
                    if (ir instanceof LiVIR liVIR) {
                        VReg target = liVIR.target;
                        if (i < lastAssignMap.get(target)) {
                            target = new VReg(target.getType(), target.getSize());
                            assignMap.put(liVIR.target, target);
                            modified = true;
                        } else
                            assignMap.remove(target);
                        block.set(i, new LiVIR(target, liVIR.value));
                        continue;
                    }
                    if (ir instanceof LoadVIR loadVIR) {
                        List<VIRItem> indexes = loadVIR.indexes;
                        for (int j = 0; j < indexes.size(); j++) {
                            VIRItem index = indexes.get(j);
                            if (index instanceof VReg reg && assignMap.containsKey(reg)) {
                                reg = assignMap.get(reg);
                                indexes.set(j, reg);
                            }
                        }
                        VReg target = loadVIR.target;
                        if (i < lastAssignMap.get(target)) {
                            target = new VReg(target.getType(), target.getSize());
                            assignMap.put(loadVIR.target, target);
                            modified = true;
                        } else
                            assignMap.remove(target);
                        block.set(i, new LoadVIR(target, loadVIR.symbol, indexes));
                        continue;
                    }
                    if (ir instanceof MovVIR movVIR) {
                        VReg source = movVIR.source;
                        if (assignMap.containsKey(source))
                            source = assignMap.get(source);
                        VReg target = movVIR.target;
                        if (i < lastAssignMap.get(target)) {
                            target = new VReg(target.getType(), target.getSize());
                            assignMap.put(movVIR.target, target);
                            modified = true;
                        } else
                            assignMap.remove(target);
                        block.set(i, new MovVIR(target, source));
                        continue;
                    }
                    if (ir instanceof RetVIR retVIR) {
                        VIRItem retVal = retVIR.retVal;
                        if (retVal instanceof VReg reg && assignMap.containsKey(reg))
                            retVal = assignMap.get(reg);
                        block.set(i, new RetVIR(retVal));
                        continue;
                    }
                    if (ir instanceof StoreVIR storeVIR) {
                        List<VIRItem> indexes = storeVIR.indexes;
                        for (int j = 0; j < indexes.size(); j++) {
                            VIRItem index = indexes.get(j);
                            if (index instanceof VReg reg && assignMap.containsKey(reg)) {
                                reg = assignMap.get(reg);
                                indexes.set(j, reg);
                            }
                        }
                        VReg source = storeVIR.source;
                        if (assignMap.containsKey(source))
                            source = assignMap.get(source);
                        block.set(i, new StoreVIR(storeVIR.symbol, indexes, source));
                        continue;
                    }
                    if (ir instanceof UnaryVIR unaryVIR) {
                        VIRItem source = unaryVIR.source;
                        if (source instanceof VReg reg && assignMap.containsKey(reg))
                            source = assignMap.get(reg);
                        VReg target = unaryVIR.target;
                        if (i < lastAssignMap.get(target)) {
                            target = new VReg(target.getType(), target.getSize());
                            assignMap.put(unaryVIR.target, target);
                            modified = true;
                        } else
                            assignMap.remove(target);
                        block.set(i, new UnaryVIR(unaryVIR.type, target, source));
                        continue;
                    }
                }
            }
        }
        return modified;
    }
}
