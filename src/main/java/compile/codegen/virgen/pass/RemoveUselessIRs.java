package compile.codegen.virgen.pass;

import compile.codegen.virgen.Block;
import compile.codegen.virgen.VReg;
import compile.codegen.virgen.VirtualFunction;
import compile.codegen.virgen.vir.*;
import compile.symbol.GlobalSymbol;
import compile.symbol.LocalSymbol;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RemoveUselessIRs extends Pass {
    public RemoveUselessIRs(Set<GlobalSymbol> globals, Map<String, VirtualFunction> funcs) {
        super(globals, funcs);
    }

    @Override
    public boolean run() {
        boolean modified = false;
        for (VirtualFunction func : funcs.values()) {
            boolean innerModified;
            do {
                innerModified = removeUnusedRegIRs(func);
                innerModified |= removeUnusedSymbolIRs(func);
                modified |= innerModified;
            } while (innerModified);
        }
        return modified;
    }

    private boolean removeUnusedRegIRs(VirtualFunction func) {
        boolean modified = false;
        Set<VReg> usedRegs = analyzeUsedRegs(func);
        for (Block block : func.getBlocks()) {
            for (int i = 0; i < block.size(); i++) {
                VIR ir = block.get(i);
                if (ir instanceof BinaryVIR || ir instanceof LoadVIR || ir instanceof LiVIR || ir instanceof MovVIR || ir instanceof UnaryVIR) {
                    if (!usedRegs.contains(ir.getWrite())) {
                        block.remove(i);
                        i--;
                        modified = true;
                    }
                }
            }
        }
        return modified;
    }

    private boolean removeUnusedSymbolIRs(VirtualFunction func) {
        boolean modified = false;
        Set<LocalSymbol> usedLocals = analyzeUsedLocals(func);
        for (Block block : func.getBlocks()) {
            for (int i = 0; i < block.size(); i++) {
                VIR ir = block.get(i);
                if (ir instanceof StoreVIR storeVIR && storeVIR.symbol instanceof LocalSymbol local) {
                    if (!usedLocals.contains(local)) {
                        block.remove(i);
                        i--;
                        modified = true;
                    }
                }
            }
        }
        return modified;
    }

    private Set<VReg> analyzeUsedRegs(VirtualFunction func) {
        Set<VReg> usedRegs = new HashSet<>();
        for (Block block : func.getBlocks())
            for (VIR ir : block)
                usedRegs.addAll(ir.getRead());
        return usedRegs;
    }

    private Set<LocalSymbol> analyzeUsedLocals(VirtualFunction func) {
        Set<LocalSymbol> usedLocals = new HashSet<>();
        for (Block block : func.getBlocks())
            for (VIR ir : block)
                if (ir instanceof LoadVIR loadVIR && loadVIR.symbol instanceof LocalSymbol local)
                    usedLocals.add(local);
        return usedLocals;
    }
}
