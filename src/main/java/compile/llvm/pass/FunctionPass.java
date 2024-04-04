package compile.llvm.pass;

import compile.llvm.Function;
import compile.llvm.Module;

public abstract class FunctionPass extends Pass {
    public FunctionPass(Module module) {
        super(module);
    }

    @Override
    public boolean run() {
        boolean modified = false;
        for (Function func : module.getFunctions()) {
            modified |= runOnFunction(func);
        }
        return modified;
    }

    public abstract boolean runOnFunction(Function function);
}
