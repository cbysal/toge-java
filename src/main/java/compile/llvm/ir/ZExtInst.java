package compile.llvm.ir;

import compile.llvm.BasicBlock;
import compile.llvm.type.Type;
import compile.llvm.value.Value;

public class ZExtInst extends CastInst {
    public ZExtInst(BasicBlock block, Type type, Value value) {
        super(block, type, value);
    }
}
