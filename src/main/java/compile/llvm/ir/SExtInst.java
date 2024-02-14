package compile.llvm.ir;

import compile.llvm.type.Type;
import compile.llvm.value.Value;

public class SExtInst extends CastInst {
    public SExtInst(Type type, Value value) {
        super(type, value);
    }
}
