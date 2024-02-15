package compile.llvm;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Module {
    private final Map<String, GlobalVariable> globals = new HashMap<>();
    private final Map<String, Function> functions = new HashMap<>();

    public void addGlobal(GlobalVariable global) {
        globals.put(global.getName(), global);
    }

    public void addFunction(Function function) {
        functions.put(function.getName(), function);
    }

    public boolean hasGlobal() {
        return !globals.isEmpty();
    }

    public boolean hasFunction() {
        return !functions.isEmpty();
    }

    public GlobalVariable getGlobal(String name) {
        return globals.get(name);
    }

    public Function getFunction(String name) {
        return functions.get(name);
    }

    public Collection<GlobalVariable> getGlobals() {
        return globals.values();
    }

    public Collection<Function> getFunctions() {
        return functions.values();
    }
}
