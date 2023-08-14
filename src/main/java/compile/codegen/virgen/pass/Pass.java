package compile.codegen.virgen.pass;

import compile.codegen.virgen.VirtualFunction;
import compile.symbol.GlobalSymbol;

import java.util.Map;
import java.util.Set;

public abstract class Pass {
    protected final Set<GlobalSymbol> globals;
    protected final Map<String, VirtualFunction> funcs;

    public Pass(Set<GlobalSymbol> globals, Map<String, VirtualFunction> funcs) {
        this.globals = globals;
        this.funcs = funcs;
    }

    public abstract boolean run();
}
