package compile.llvm.pass;

import compile.llvm.Module;

public abstract class Pass implements Runnable {
    protected final Module module;

    public Pass(Module module) {
        this.module = module;
    }
}
