package compile.codegen.virgen.pass;

import compile.codegen.virgen.Block;
import compile.codegen.virgen.VReg;
import compile.codegen.virgen.VirtualFunction;
import compile.codegen.virgen.vir.LoadVIR;
import compile.codegen.virgen.vir.StoreVIR;
import compile.codegen.virgen.vir.VIR;
import compile.codegen.virgen.vir.VIRItem;
import compile.symbol.GlobalSymbol;
import compile.symbol.Type;
import compile.symbol.Value;

import java.util.*;
import java.util.stream.Collectors;

public class SplitGlobals extends Pass {
    public SplitGlobals(Set<GlobalSymbol> globals, Map<String, VirtualFunction> funcs) {
        super(globals, funcs);
    }

    @Override
    public boolean run() {
        Set<GlobalSymbol> toRemoveGlobals =
                globals.stream().filter(global -> !global.isSingle()).collect(Collectors.toSet());
        for (VirtualFunction func : funcs.values()) {
            for (Block block : func.getBlocks()) {
                for (VIR ir : block) {
                    if (ir instanceof LoadVIR loadVIR && loadVIR.symbol instanceof GlobalSymbol global && toRemoveGlobals.contains(global)) {
                        List<VIRItem> indexes = loadVIR.indexes;
                        if (indexes.isEmpty() || indexes.get(0) instanceof VReg)
                            toRemoveGlobals.remove(global);
                        continue;
                    }
                    if (ir instanceof StoreVIR storeVIR && storeVIR.symbol instanceof GlobalSymbol global && toRemoveGlobals.contains(global)) {
                        List<VIRItem> indexes = storeVIR.indexes;
                        if (indexes.isEmpty() || indexes.get(0) instanceof VReg)
                            toRemoveGlobals.remove(global);
                        continue;
                    }
                }
            }
        }
        if (toRemoveGlobals.isEmpty())
            return false;
        globals.removeAll(toRemoveGlobals);
        Map<GlobalSymbol, List<GlobalSymbol>> newGlobalMap = new HashMap<>();
        for (GlobalSymbol global : toRemoveGlobals) {
            List<GlobalSymbol> newGlobals = new ArrayList<>();
            newGlobalMap.put(global, newGlobals);
            if (global.getDimensions().size() == 1) {
                int dimension = global.getDimensions().get(0);
                for (int i = 0; i < dimension; i++) {
                    if (global.getType() == Type.INT)
                        newGlobals.add(new GlobalSymbol(global.isConst(), global.getType(),
                                global.getName() + "." + i, global.getInt(i)));
                    else
                        newGlobals.add(new GlobalSymbol(global.isConst(), global.getType(),
                                global.getName() + "." + i, global.getFloat(i)));
                }
            } else {
                List<Integer> oldDimensions = global.getDimensions();
                List<Map<Integer, Integer>> initMapList = new ArrayList<>();
                for (int i = 0; i < oldDimensions.get(0); i++)
                    initMapList.add(new HashMap<>());
                Map<Integer, Integer> initMap = global.getValues();
                int newSize = global.size() / 4 / oldDimensions.get(0);
                for (Map.Entry<Integer, Integer> entry : initMap.entrySet()) {
                    int index = entry.getKey() / newSize;
                    initMapList.get(index).put(entry.getKey() % newSize, entry.getValue());
                }
                for (int i = 0; i < oldDimensions.get(0); i++) {
                    newGlobals.add(new GlobalSymbol(global.isConst(), global.getType(), global.getName() + "." + i,
                            oldDimensions.subList(1, oldDimensions.size()), initMapList.get(i)));
                }
            }
        }
        Set<GlobalSymbol> toAddGlobals = new HashSet<>();
        for (VirtualFunction func : funcs.values()) {
            for (Block block : func.getBlocks()) {
                for (int i = 0; i < block.size(); i++) {
                    VIR ir = block.get(i);
                    if (ir instanceof LoadVIR loadVIR && loadVIR.symbol instanceof GlobalSymbol global && newGlobalMap.containsKey(global)) {
                        List<VIRItem> dimensions = loadVIR.indexes;
                        GlobalSymbol newGlobal = newGlobalMap.get(global).get(((Value) dimensions.get(0)).getInt());
                        block.set(i, new LoadVIR(loadVIR.target, newGlobal, dimensions.subList(1, dimensions.size())));
                        toAddGlobals.add(newGlobal);
                        continue;
                    }
                    if (ir instanceof StoreVIR storeVIR && storeVIR.symbol instanceof GlobalSymbol global && newGlobalMap.containsKey(global)) {
                        List<VIRItem> dimensions = storeVIR.indexes;
                        GlobalSymbol newGlobal = newGlobalMap.get(global).get(((Value) dimensions.get(0)).getInt());
                        block.set(i, new StoreVIR(newGlobal, dimensions.subList(1, dimensions.size()),
                                storeVIR.source));
                        toAddGlobals.add(newGlobal);
                        continue;
                    }
                }
            }
        }
        globals.addAll(toAddGlobals);
        return true;
    }
}
