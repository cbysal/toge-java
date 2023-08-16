package compile.codegen.virgen.pass;

import compile.codegen.virgen.Block;
import compile.codegen.virgen.VReg;
import compile.codegen.virgen.VirtualFunction;
import compile.codegen.virgen.vir.MovVIR;
import compile.codegen.virgen.vir.PhiVIR;
import compile.codegen.virgen.vir.VIR;
import compile.symbol.GlobalSymbol;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class DegradePhi extends Pass {
    public DegradePhi(Set<GlobalSymbol> globals, Map<String, VirtualFunction> funcs) {
        super(globals, funcs);
    }

    @Override
    public boolean run() {
        boolean modified = false;
        for (VirtualFunction func : funcs.values()) {
            Map<VReg, Block> retToBlockMap = new HashMap<>();
            for (Block block : func.getBlocks())
                for (VIR ir : block)
                    if (ir.getWrite() != null)
                        retToBlockMap.put(ir.getWrite(), block);
            for (Block block : func.getBlocks()) {
                for (int i = 0; i < block.size(); i++) {
                    VIR ir = block.get(i);
                    if (ir instanceof PhiVIR phiVIR){
                        VReg target = phiVIR.target();
                        Set<VReg> sources = phiVIR.sources();
                        if (sources.size() == 1) {
                            VReg source = sources.iterator().next();
                            Block sourceBlock = retToBlockMap.get(source);
                            sourceBlock.add(sourceBlock.size() - 1, new MovVIR(target, source));
                            block.remove(i);
                            i--;
                            modified = true;
                        }
                        continue;
                    }
                    break;
                }
            }
        }
        return modified;
    }
}
