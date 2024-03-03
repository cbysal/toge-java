package compile.llvm.ir;

import compile.llvm.BasicBlock;
import compile.llvm.type.Type;
import compile.llvm.value.Value;

public class SExtInst extends CastInst {
    public SExtInst(BasicBlock block, Type type, Value value) {
        super(block, type, value);
    }
}
