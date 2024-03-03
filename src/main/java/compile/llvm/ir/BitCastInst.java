package compile.llvm.ir;

import compile.llvm.BasicBlock;
import compile.llvm.type.Type;
import compile.llvm.value.Value;

public class BitCastInst extends CastInst {
    public BitCastInst(BasicBlock block, Type type, Value operand) {
        super(block, type, operand);
    }
}
