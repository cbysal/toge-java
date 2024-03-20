package compile.llvm.pass;

import compile.llvm.Function;
import compile.llvm.Module;

public abstract class FunctionPass extends Pass {
    public FunctionPass(Module module) {
        super(module);
    }

    @Override
    public void run() {
        module.getFunctions().forEach(this::runOnFunction);
    }

    public abstract void runOnFunction(Function function);
}
