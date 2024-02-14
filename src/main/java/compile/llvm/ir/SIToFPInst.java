package compile.llvm.ir;

import compile.llvm.type.Type;
import compile.llvm.value.Value;

public class SIToFPInst extends CastInst {
    public SIToFPInst(Type type, Value value) {
        super(type, value);
    }
}
