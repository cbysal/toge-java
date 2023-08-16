package compile.codegen.virgen.pass;

import compile.codegen.virgen.Block;
import compile.codegen.virgen.VReg;
import compile.codegen.virgen.VirtualFunction;
import compile.codegen.virgen.vir.*;
import compile.symbol.GlobalSymbol;
import compile.symbol.ParamSymbol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ParamToReg extends Pass {
    public ParamToReg(Set<GlobalSymbol> globals, Map<String, VirtualFunction> funcs) {
        super(globals, funcs);
    }

    @Override
    public boolean run() {
        boolean modified = false;
        for (VirtualFunction func : funcs.values()) {
            Map<ParamSymbol, Integer> counter = new HashMap<>();
            for (Block block : func.getBlocks()) {
                for (VIR ir : block) {
                    if (ir instanceof LoadVIR loadVIR && loadVIR.symbol() instanceof ParamSymbol param && param.isSingle()) {
                        counter.put(param, counter.getOrDefault(param, 0) + 1);
                        continue;
                    }
                    if (ir instanceof StoreVIR storeVIR && storeVIR.symbol() instanceof ParamSymbol param && param.isSingle()) {
                        counter.put(param, counter.getOrDefault(param, 0) + 1);
                        continue;
                    }
                }
            }
            Set<ParamSymbol> params =
                    counter.entrySet().stream().filter(e -> e.getValue() > 1).map(Map.Entry::getKey).collect(Collectors.toSet());
            Map<ParamSymbol, VReg> paramToRegMap = new HashMap<>();
            for (ParamSymbol param : params) {
                VReg reg = new VReg(param.getType(), 4);
                paramToRegMap.put(param, reg);
            }
            Block headBlock = new Block();
            for (Map.Entry<ParamSymbol, VReg> entry : paramToRegMap.entrySet())
                headBlock.add(new LoadVIR(entry.getValue(), entry.getKey(), List.of()));
            headBlock.add(new JumpVIR(func.getBlocks().get(0)));
            List<Block> blocks = func.getBlocks();
            for (Block block : blocks) {
                for (int i = 0; i < block.size(); i++) {
                    VIR ir = block.get(i);
                    if (ir instanceof LoadVIR loadVIR && loadVIR.symbol() instanceof ParamSymbol param && paramToRegMap.containsKey(param)) {
                        block.set(i, new MovVIR(loadVIR.target(), paramToRegMap.get(param)));
                        modified = true;
                    }
                    if (ir instanceof StoreVIR storeVIR && storeVIR.symbol() instanceof ParamSymbol param && paramToRegMap.containsKey(param)) {
                        block.set(i, new MovVIR(paramToRegMap.get(param), storeVIR.source()));
                        modified = true;
                    }
                }
            }
            blocks.add(0, headBlock);
        }
        return modified;
    }
}
