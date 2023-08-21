package compile.codegen.mirgen;

import compile.codegen.mirgen.pass.CommonSubexpressionElimination;
import compile.codegen.mirgen.pass.PeekHole;
import compile.symbol.GlobalSymbol;

import java.util.Map;
import java.util.Set;

public class MIRPassManager {
    private final Set<GlobalSymbol> globals;
    private final Map<String, MachineFunction> funcs;

    public MIRPassManager(Set<GlobalSymbol> globals, Map<String, MachineFunction> funcs) {
        this.globals = globals;
        this.funcs = funcs;
    }

    public void run() {
        boolean toContinue;
        do {
            toContinue = new PeekHole(globals, funcs).run();
        } while (toContinue);
    }
}
