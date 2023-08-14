package compile.codegen.virgen.pass;

import compile.codegen.virgen.Block;
import compile.codegen.virgen.VirtualFunction;
import compile.codegen.virgen.vir.LiVIR;
import compile.codegen.virgen.vir.LoadVIR;
import compile.codegen.virgen.vir.StoreVIR;
import compile.codegen.virgen.vir.VIR;
import compile.symbol.DataSymbol;
import compile.symbol.GlobalSymbol;
import compile.symbol.Type;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GlobalToImm extends Pass {
    public GlobalToImm(Set<GlobalSymbol> globals, Map<String, VirtualFunction> funcs) {
        super(globals, funcs);
    }

    @Override
    public boolean run() {
        Set<GlobalSymbol> toRemoveGlobals = globals.stream().filter(DataSymbol::isSingle).collect(Collectors.toSet());
        for (VirtualFunction func : funcs.values())
            for (Block block : func.getBlocks())
                for (VIR ir : block)
                    if (ir instanceof StoreVIR storeVIR && storeVIR.symbol() instanceof GlobalSymbol global && global.isSingle())
                        toRemoveGlobals.remove(global);
        if (toRemoveGlobals.isEmpty())
            return false;
        for (VirtualFunction func : funcs.values())
            for (Block block : func.getBlocks())
                for (int i = 0; i < block.size(); i++) {
                    VIR ir = block.get(i);
                    if (ir instanceof LoadVIR loadVIR && loadVIR.symbol() instanceof GlobalSymbol global && toRemoveGlobals.contains(global)) {
                        if (global.getType() == Type.INT)
                            block.set(i, new LiVIR(loadVIR.target(), global.getInt()));
                        else
                            block.set(i, new LiVIR(loadVIR.target(), global.getFloat()));
                    }
                }
        globals.removeAll(toRemoveGlobals);
        return true;
    }
}
