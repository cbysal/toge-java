package compile.codegen.virgen.pass;

import compile.codegen.virgen.VirtualFunction;
import compile.symbol.GlobalSymbol;

import java.util.Map;
import java.util.Set;

public class MatchPatterns extends Pass {
    public MatchPatterns(Set<GlobalSymbol> globals, Map<String, VirtualFunction> funcs) {
        super(globals, funcs);
    }

    @Override
    public boolean run() {
        boolean modified = matchAbs();
        return modified;
    }

    private boolean matchAbs() {
        // TODO wait for other optimizations
        return false;
    }
}
