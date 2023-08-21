package compile.codegen.virgen.pass;

import compile.codegen.virgen.Block;
import compile.codegen.virgen.VReg;
import compile.codegen.virgen.VirtualFunction;
import compile.codegen.virgen.vir.*;
import compile.symbol.DataSymbol;
import compile.symbol.GlobalSymbol;
import compile.symbol.LocalSymbol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GlobalToLocal extends Pass {
    public GlobalToLocal(Set<GlobalSymbol> globals, Map<String, VirtualFunction> funcs) {
        super(globals, funcs);
    }

    @Override
    public boolean run() {
        Set<GlobalSymbol> toTransferGlobals = globals.stream().filter(DataSymbol::isSingle).collect(Collectors.toSet());
        VirtualFunction mainFunc = null;
        for (VirtualFunction func : funcs.values()) {
            if (func.getSymbol().getName().equals("main")) {
                mainFunc = func;
                continue;
            }
            for (Block block : func.getBlocks()) {
                for (VIR ir : block) {
                    if (ir instanceof LoadVIR loadVIR && loadVIR.symbol instanceof GlobalSymbol global) {
                        toTransferGlobals.remove(global);
                        continue;
                    }
                    if (ir instanceof StoreVIR storeVIR && storeVIR.symbol instanceof GlobalSymbol global) {
                        toTransferGlobals.remove(global);
                        continue;
                    }
                }
            }
        }
        if (toTransferGlobals.isEmpty())
            return false;
        Map<GlobalSymbol, LocalSymbol> globalToLocalMap = new HashMap<>();
        for (GlobalSymbol global : toTransferGlobals) {
            LocalSymbol local = new LocalSymbol(global.getType(), global.getName());
            mainFunc.addLocal(local);
            globalToLocalMap.put(global, local);
        }
        for (Block block : mainFunc.getBlocks()) {
            for (int i = 0; i < block.size(); i++) {
                VIR ir = block.get(i);
                if (ir instanceof LoadVIR loadVIR && loadVIR.symbol instanceof GlobalSymbol global && globalToLocalMap.containsKey(global)) {
                    block.set(i, new LoadVIR(loadVIR.target, globalToLocalMap.get(global), loadVIR.indexes));
                    continue;
                }
                if (ir instanceof StoreVIR storeVIR && storeVIR.symbol instanceof GlobalSymbol global && globalToLocalMap.containsKey(global)) {
                    block.set(i, new StoreVIR(globalToLocalMap.get(global), storeVIR.indexes, storeVIR.source));
                    continue;
                }
            }
        }
        Block newBlock = new Block();
        for (Map.Entry<GlobalSymbol, LocalSymbol> entry : globalToLocalMap.entrySet()) {
            VReg reg = new VReg(entry.getValue().getType(), 4);
            newBlock.add(new LiVIR(reg, entry.getKey().getInt()));
            newBlock.add(new StoreVIR(entry.getValue(), List.of(), reg));
        }
        newBlock.add(new JumpVIR(mainFunc.getBlocks().get(0)));
        mainFunc.addBlock(0, newBlock);
        globals.removeAll(toTransferGlobals);
        return true;
    }
}
