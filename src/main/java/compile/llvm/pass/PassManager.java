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
            modified |= new BranchOptPass(module).run();
            modified |= new PromotePass(module).run();
            modified |= new ConstPropPass(module).run();
            modified |= new DCEPass(module).run();
        } while (modified);
    }
}
