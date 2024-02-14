package compile.llvm.ir;

import compile.llvm.type.Type;
import compile.llvm.value.Value;

public class ZExtInst extends CastInst {
    public ZExtInst(Type type, Value value) {
        super(type, value);
    }
}
