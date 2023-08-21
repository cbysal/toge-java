package compile.codegen.mirgen.pass;

import compile.codegen.Reg;
import compile.codegen.mirgen.MReg;
import compile.codegen.mirgen.MachineFunction;
import compile.codegen.mirgen.mir.*;
import compile.symbol.GlobalSymbol;
import compile.symbol.Symbol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommonSubexpressionElimination extends Pass {
    private record Item(Item.Op op, List<Reg> regs, List<Integer> values, Symbol symbol) {
        public enum Op {
            LLA
        }
    }

    public CommonSubexpressionElimination(Set<GlobalSymbol> globals, Map<String, MachineFunction> funcs) {
        super(globals, funcs);
    }

    @Override
    public boolean run() {
        boolean modified = false;
        for (MachineFunction func : funcs.values()) {
            Map<Item, Reg> replaceMap = new HashMap<>();
            List<MIR> irs = func.getIrs();
            for (int i = 0; i < irs.size(); i++) {
                MIR ir = irs.get(i);
                if (ir instanceof CallMIR) {
                    replaceMap.entrySet().removeIf(entry -> MReg.I_CALLER_REGS.contains(entry.getValue()) || MReg.F_CALLER_REGS.contains(entry.getValue()));
                    continue;
                }
                if (ir instanceof LabelMIR) {
                    replaceMap.clear();
                    continue;
                }
                if (ir instanceof LlaMIR llaMIR) {
                    Reg dest = llaMIR.dest;
                    Item item = new Item(Item.Op.LLA, List.of(), List.of(), llaMIR.symbol);
                    if (replaceMap.containsKey(item)) {
                        irs.set(i, new RrMIR(RrMIR.Op.MV, dest, replaceMap.get(item)));
                        modified = true;
                        continue;
                    }
                    replaceMap.put(item, dest);
                    continue;
                }
            }
        }
        return modified;
    }
}
