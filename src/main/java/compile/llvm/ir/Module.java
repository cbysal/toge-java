package compile.llvm.ir;

import java.util.HashMap;
import java.util.Map;

public class Module {
    private final Map<String, Global> globals = new HashMap<>();
    private final Map<String, Function> declares = new HashMap<>();
    private final Map<String, Function> functions = new HashMap<>();

    public void addGlobal(Global global) {
        globals.put(global.getName(), global);
    }

    public void addDeclare(Function function) {
        declares.put(function.getName(), function);
    }

    public void addFunction(Function function) {
        functions.put(function.getName(), function);
    }

    public Global getGlobal(String name) {
        return globals.get(name);
    }

    public Map<String, Global> getGlobals() {
        return globals;
    }

    public Map<String, Function> getFunctions() {
        return functions;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Function declare : declares.values()) {
            builder.append(declare.getDeclare());
        }
        builder.append('\n');
        for (Global global : globals.values()) {
            builder.append(global).append('\n');
        }
        builder.append('\n');
        for (Function function : functions.values()) {
            builder.append(function);
        }
        return builder.toString();
    }
}
