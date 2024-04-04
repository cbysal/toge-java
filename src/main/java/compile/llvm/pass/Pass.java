package compile.llvm.pass;

import compile.llvm.Module;

public abstract class Pass {
    protected final Module module;

    public Pass(Module module) {
        this.module = module;
    }

    public abstract boolean run();
}
