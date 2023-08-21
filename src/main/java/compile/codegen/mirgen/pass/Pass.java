package compile.codegen.mirgen.pass;

import compile.codegen.mirgen.MachineFunction;
import compile.symbol.GlobalSymbol;

import java.util.Map;
import java.util.Set;

public abstract class Pass {
    protected final Set<GlobalSymbol> globals;
    protected final Map<String, MachineFunction> funcs;

    public Pass(Set<GlobalSymbol> globals, Map<String, MachineFunction> funcs) {
        this.globals = globals;
        this.funcs = funcs;
    }

    public abstract boolean run();
}
