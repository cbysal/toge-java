package compile.llvm.ir;

import compile.llvm.BasicBlock;
import compile.llvm.type.Type;
import compile.llvm.value.Value;

public class SIToFPInst extends CastInst {
    public SIToFPInst(BasicBlock block, Type type, Value value) {
        super(block, type, value);
    }
}
