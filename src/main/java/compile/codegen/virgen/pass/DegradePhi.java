package compile.codegen.virgen.pass;

import compile.codegen.virgen.Block;
import compile.codegen.virgen.VReg;
import compile.codegen.virgen.VirtualFunction;
import compile.codegen.virgen.vir.MovVIR;
import compile.codegen.virgen.vir.VIR;
import compile.symbol.GlobalSymbol;

import java.util.*;

public class DegradePhi extends Pass {
    public DegradePhi(Set<GlobalSymbol> globals, Map<String, VirtualFunction> funcs) {
        super(globals, funcs);
    }

    @Override
    public boolean run() {
        boolean modified = false;
        for (VirtualFunction func : funcs.values()) {
            for (Block block : func.getBlocks()) {
                List<VIR> newIRs = new ArrayList<>();
                Iterator<Map.Entry<VReg, Set<VReg>>> iterator = block.getPhiMap().entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<VReg, Set<VReg>> entry = iterator.next();
                    VReg target = entry.getKey();
                    Set<VReg> sources = entry.getValue();
                    if (sources.size() == 1) {
                        VReg source = sources.iterator().next();
                        newIRs.add(new MovVIR(target, source));
                        iterator.remove();
                        modified = true;
                    }
                }
                block.addAll(0, newIRs);
            }
        }
        return modified;
    }
}
