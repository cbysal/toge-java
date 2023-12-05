package compile.codegen.virgen.pass;

import compile.codegen.virgen.Block;
import compile.codegen.virgen.VReg;
import compile.codegen.virgen.VirtualFunction;
import compile.codegen.virgen.vir.*;
import compile.symbol.GlobalSymbol;
import compile.symbol.Value;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CombineInstructions extends Pass {
    public CombineInstructions(Set<GlobalSymbol> globals, Map<String, VirtualFunction> funcs) {
        super(globals, funcs);
    }

    @Override
    public boolean run() {
        boolean modified = false;
        for (VirtualFunction func : funcs.values()) {
            for (Block block : func.getBlocks()) {
                Map<VReg, Pair<VReg, Value>> mulIRs = new HashMap<>();
                for (int i = 0; i < block.size(); i++) {
                    VIR ir = block.get(i);
                    if (ir instanceof BinaryVIR binaryVIR) {
                        if (binaryVIR.type == BinaryVIR.Type.ADD) {
                            if (binaryVIR.left instanceof VReg reg1 && binaryVIR.right instanceof VReg reg2) {
                                if (reg1 == reg2) {
                                    block.set(i, new BinaryVIR(BinaryVIR.Type.MUL, binaryVIR.target, reg1, new Value(2)));
                                    mulIRs.put(binaryVIR.target, Pair.of(reg1, new Value(2)));
                                    modified = true;
                                    continue;
                                }
                                if (mulIRs.containsKey(reg1)) {
                                    Pair<VReg, Value> regValue = mulIRs.get(reg1);
                                    if (regValue.getLeft() == reg2) {
                                        block.set(i, new BinaryVIR(BinaryVIR.Type.MUL, binaryVIR.target, regValue.getLeft(), new Value(regValue.getRight().intValue() + 1)));
                                        mulIRs.put(binaryVIR.target, Pair.of(regValue.getLeft(), new Value(regValue.getRight().intValue() + 1)));
                                        modified = true;
                                    } else {
                                        mulIRs.remove(binaryVIR.target);
                                    }
                                    continue;
                                }
                                if (mulIRs.containsKey(reg2)) {
                                    Pair<VReg, Value> regValue = mulIRs.get(reg2);
                                    if (regValue.getLeft() == reg1) {
                                        block.set(i, new BinaryVIR(BinaryVIR.Type.MUL, binaryVIR.target, regValue.getLeft(), new Value(regValue.getRight().intValue() + 1)));
                                        mulIRs.put(binaryVIR.target, Pair.of(regValue.getLeft(), new Value(regValue.getRight().intValue() + 1)));
                                        modified = true;
                                    } else {
                                        mulIRs.remove(binaryVIR.target);
                                    }
                                    continue;
                                }
                            }
                            mulIRs.remove(binaryVIR.target);
                        } else if (binaryVIR.type == BinaryVIR.Type.MUL) {
                            if (binaryVIR.left instanceof VReg reg && binaryVIR.right instanceof Value value) {
                                mulIRs.put(binaryVIR.target, Pair.of(reg, value));
                            } else if (binaryVIR.left instanceof Value value && binaryVIR.right instanceof VReg reg) {
                                mulIRs.put(binaryVIR.target, Pair.of(reg, value));
                            }
                        } else if (binaryVIR.type == BinaryVIR.Type.DIV) {
                            if (binaryVIR.left instanceof VReg reg && binaryVIR.right instanceof Value value && mulIRs.containsKey(reg)) {
                                Pair<VReg, Value> regValue = mulIRs.get(reg);
                                if (regValue.getRight().equals(value)) {
                                    block.set(i, new MovVIR(binaryVIR.target, regValue.getLeft()));
                                    modified = true;
                                }
                            }
                            mulIRs.remove(binaryVIR.target);
                        } else {
                            mulIRs.remove(binaryVIR.target);
                        }
                        continue;
                    }
                    if (ir instanceof CallVIR callVIR) {
                        if (callVIR.target != null)
                            mulIRs.remove(callVIR.target);
                        continue;
                    }
                    if (ir instanceof LiVIR liVIR) {
                        mulIRs.remove(liVIR.target);
                        continue;
                    }
                    if (ir instanceof LoadVIR loadVIR) {
                        mulIRs.remove(loadVIR.target);
                        continue;
                    }
                    if (ir instanceof MovVIR movVIR) {
                        mulIRs.remove(movVIR.target);
                        continue;
                    }
                    if (ir instanceof UnaryVIR unaryVIR) {
                        mulIRs.remove(unaryVIR.target);
                        continue;
                    }
                }
            }
        }
        return modified;
    }
}
