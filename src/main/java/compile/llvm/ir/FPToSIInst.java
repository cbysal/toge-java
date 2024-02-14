package compile.llvm.ir;

import compile.llvm.type.Type;
import compile.llvm.value.Value;

public class FPToSIInst extends CastInst {
    public FPToSIInst(Type type, Value operand) {
        super(type, operand);
    }
}
