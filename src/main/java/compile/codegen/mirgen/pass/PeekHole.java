package compile.codegen.mirgen.pass;

import compile.codegen.mirgen.MachineFunction;
import compile.codegen.mirgen.mir.BMIR;
import compile.codegen.mirgen.mir.LabelMIR;
import compile.codegen.mirgen.mir.MIR;
import compile.symbol.GlobalSymbol;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class PeekHole extends Pass {
    public PeekHole(Set<GlobalSymbol> globals, Map<String, MachineFunction> funcs) {
        super(globals, funcs);
    }

    @Override
    public boolean run() {
        boolean modified = false;
        for (MachineFunction func : funcs.values()) {
            List<MIR> irs = func.getIrs();
            for (int i = 0; i < irs.size() - 1; i++) {
                MIR ir1 = irs.get(i);
                MIR ir2 = irs.get(i + 1);
                if (ir1 instanceof BMIR bMIR && ir2 instanceof LabelMIR labelMIR) {
                    if (bMIR.label == labelMIR.label) {
                        irs.remove(i);
                        i--;
                    }
                    continue;
                }
            }
        }
        return modified;
    }
}
