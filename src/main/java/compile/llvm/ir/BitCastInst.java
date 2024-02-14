package compile.llvm.ir;

import compile.llvm.type.Type;
import compile.llvm.value.Value;

public class BitCastInst extends CastInst {
    public BitCastInst(Type type, Value operand) {
        super(type, operand);
    }
}
