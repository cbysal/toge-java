package compile.codegen.virgen.pass;

import compile.codegen.virgen.Block;
import compile.codegen.virgen.VReg;
import compile.codegen.virgen.VirtualFunction;
import compile.codegen.virgen.vir.LoadVIR;
import compile.codegen.virgen.vir.StoreVIR;
import compile.codegen.virgen.vir.VIR;
import compile.codegen.virgen.vir.VIRItem;
import compile.symbol.GlobalSymbol;
import compile.symbol.LocalSymbol;
import compile.symbol.Value;

import java.util.*;
import java.util.stream.Collectors;

public class SplitLocals extends Pass {
    public SplitLocals(Set<GlobalSymbol> globals, Map<String, VirtualFunction> funcs) {
        super(globals, funcs);
    }

    @Override
    public boolean run() {
        boolean modified = false;
        for (VirtualFunction func : funcs.values()) {
            Set<LocalSymbol> locals =
                    func.getLocals().stream().filter(local -> !local.isSingle()).collect(Collectors.toSet());
            for (Block block : func.getBlocks()) {
                for (VIR ir : block) {
                    if (ir instanceof LoadVIR loadVIR && loadVIR.symbol instanceof LocalSymbol local && locals.contains(local)) {
                        List<VIRItem> indexes = loadVIR.indexes;
                        if (indexes.isEmpty() || indexes.get(0) instanceof VReg)
                            locals.remove(local);
                        continue;
                    }
                    if (ir instanceof StoreVIR storeVIR && storeVIR.symbol instanceof LocalSymbol local && locals.contains(local)) {
                        List<VIRItem> indexes = storeVIR.indexes;
                        if (indexes.isEmpty() || indexes.get(0) instanceof VReg)
                            locals.remove(local);
                        continue;
                    }
                }
            }
            func.getLocals().removeAll(locals);
            Map<LocalSymbol, List<LocalSymbol>> newLocalMap = new HashMap<>();
            for (LocalSymbol local : locals) {
                List<LocalSymbol> newLocals = new ArrayList<>();
                newLocalMap.put(local, newLocals);
                if (local.getDimensions().size() == 1) {
                    int dimension = local.getDimensions().get(0);
                    for (int i = 0; i < dimension; i++)
                        newLocals.add(new LocalSymbol(local.getType(), local.getName() + "." + i));
                } else {
                    List<Integer> oldDimensions = local.getDimensions();
                    for (int i = 0; i < oldDimensions.get(0); i++) {
                        newLocals.add(new LocalSymbol(local.getType(), local.getName() + "." + i,
                                oldDimensions.subList(1, oldDimensions.size())));
                    }
                }
            }
            newLocalMap.values().forEach(func.getLocals()::addAll);
            for (Block block : func.getBlocks()) {
                for (int i = 0; i < block.size(); i++) {
                    VIR ir = block.get(i);
                    if (ir instanceof LoadVIR loadVIR && loadVIR.symbol instanceof LocalSymbol local && newLocalMap.containsKey(local)) {
                        List<VIRItem> indexes = loadVIR.indexes;
                        block.set(i, new LoadVIR(loadVIR.target,
                                newLocalMap.get(local).get(((Value) indexes.get(0)).intValue()), indexes.subList(1,
                                indexes.size())));
                        continue;
                    }
                    if (ir instanceof StoreVIR storeVIR && storeVIR.symbol instanceof LocalSymbol local && newLocalMap.containsKey(local)) {
                        List<VIRItem> indexes = storeVIR.indexes;
                        block.set(i, new StoreVIR(newLocalMap.get(local).get(((Value) indexes.get(0)).intValue()),
                                indexes.subList(1, indexes.size()), storeVIR.source));
                        continue;
                    }
                }
            }
            modified |= !locals.isEmpty();
        }
        return modified;
    }
}
