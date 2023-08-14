package compile.codegen.virgen.pass;

import compile.codegen.virgen.Block;
import compile.codegen.virgen.VReg;
import compile.codegen.virgen.VirtualFunction;
import compile.codegen.virgen.vir.*;
import compile.symbol.GlobalSymbol;

import java.util.Map;
import java.util.Set;

public class PeekHole extends Pass {
    public PeekHole(Set<GlobalSymbol> globals, Map<String, VirtualFunction> funcs) {
        super(globals, funcs);
    }

    @Override
    public boolean run() {
        boolean modified = false;
        for (VirtualFunction func : funcs.values()) {
            for (Block block : func.getBlocks()) {
                for (int i = 0; i < block.size() - 1; i++) {
                    VIR ir1 = block.get(i);
                    VIR ir2 = block.get(i + 1);
                    if (ir1 instanceof StoreVIR storeVIR && ir2 instanceof LoadVIR loadVIR) {
                        if (storeVIR.symbol() == loadVIR.symbol() && storeVIR.indexes().equals(loadVIR.indexes())) {
                            block.set(i + 1, new MovVIR(loadVIR.target(), storeVIR.source()));
                            modified = true;
                        }
                        continue;
                    }
                    if (ir1 instanceof BinaryVIR binaryVIR1 && ir2 instanceof BinaryVIR binaryVIR2) {
                        if (binaryVIR1.type() == BinaryVIR.Type.ADD && binaryVIR2.type() == BinaryVIR.Type.SUB) {
                            if (binaryVIR1.right().equals(binaryVIR2.right()) && binaryVIR1.left() instanceof VReg source1 && binaryVIR2.left() instanceof VReg source2) {
                                VReg target1 = binaryVIR1.target();
                                VReg target2 = binaryVIR2.target();
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
}
