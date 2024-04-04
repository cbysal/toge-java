package compile.llvm.pass;

import compile.llvm.BasicBlock;
import compile.llvm.Function;
import compile.llvm.Module;
import compile.llvm.ir.CallInst;
import compile.llvm.ir.Instruction;
import compile.llvm.ir.StoreInst;

public class DCEPass extends FunctionPass {
    public DCEPass(Module module) {
        super(module);
    }

    @Override
    public boolean runOnFunction(Function func) {
        boolean modified = false;
        for (BasicBlock block : func) {
            for (int i = 0; i < block.size() - 1; i++) {
                Instruction inst = block.get(i);
                if (!(inst instanceof CallInst || inst instanceof StoreInst) && inst.getUses().isEmpty()) {
                    block.remove(i);
                    i--;
                    modified = true;
                }
            }
        }
        return modified;
    }
}
