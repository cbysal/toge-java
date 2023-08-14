package compile.codegen.virgen.pass;

import compile.codegen.virgen.Block;
import compile.codegen.virgen.VReg;
import compile.codegen.virgen.VirtualFunction;
import compile.codegen.virgen.vir.LoadVIR;
import compile.codegen.virgen.vir.MovVIR;
import compile.codegen.virgen.vir.StoreVIR;
import compile.codegen.virgen.vir.VIR;
import compile.symbol.DataSymbol;
import compile.symbol.GlobalSymbol;
import compile.symbol.LocalSymbol;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MemToReg extends Pass {
    public MemToReg(Set<GlobalSymbol> globals, Map<String, VirtualFunction> funcs) {
        super(globals, funcs);
    }

    @Override
    public boolean run() {
        boolean modified = false;
        for (VirtualFunction func : funcs.values()) {
            Set<LocalSymbol> locals =
                    func.getLocals().stream().filter(DataSymbol::isSingle).collect(Collectors.toSet());
            Map<LocalSymbol, VReg> local2Reg = new HashMap<>();
            for (Block block : func.getBlocks()) {
                for (int i = 0; i < block.size(); i++) {
                    VIR ir = block.get(i);
                    if (ir instanceof LoadVIR loadVIR && loadVIR.symbol() instanceof LocalSymbol local && locals.contains(local)) {
                        VReg reg;
                        if (local2Reg.containsKey(local))
                            reg = local2Reg.get(local);
                        else {
                            reg = new VReg(local.getType(), 4);
                            local2Reg.put(local, reg);
                        }
                        MovVIR newIR = new MovVIR(loadVIR.target(), reg);
                        block.set(i, newIR);
                        continue;
                    }
                    if (ir instanceof StoreVIR storeVIR && storeVIR.symbol() instanceof LocalSymbol local && locals.contains(local)) {
                        VReg reg;
                        if (local2Reg.containsKey(local))
                            reg = local2Reg.get(local);
                        else {
                            reg = new VReg(local.getType(), 4);
                            local2Reg.put(local, reg);
                        }
                        MovVIR newIR = new MovVIR(reg, storeVIR.source());
                        block.set(i, newIR);
                    }
                }
            }
            func.getLocals().removeAll(locals);
            modified |= !locals.isEmpty();
        }
        return modified;
    }
}
