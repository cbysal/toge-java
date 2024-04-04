package compile.llvm.pass;

import compile.llvm.Module;

public class PassManager {
    private final Module module;

    public PassManager(Module module) {
        this.module = module;
    }

    public void run() {
        boolean modified;
        do {
            modified = false;
            modified |= new PromotePass(module).run();
        } while (modified);
    }
}
