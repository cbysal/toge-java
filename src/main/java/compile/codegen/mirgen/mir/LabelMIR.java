package compile.codegen.mirgen.mir;

import compile.llvm.BasicBlock;

public class LabelMIR extends MIR {
    public final BasicBlock block;

    public LabelMIR(BasicBlock block) {
        this.block = block;
    }

    public BasicBlock getBlock() {
        return block;
    }

    @Override
    public String toString() {
        return block.getName() + ":";
    }
}
