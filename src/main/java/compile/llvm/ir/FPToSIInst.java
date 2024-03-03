package compile.llvm.ir;

import compile.llvm.BasicBlock;
import compile.llvm.type.Type;
import compile.llvm.value.Value;

public class FPToSIInst extends CastInst {
    public FPToSIInst(BasicBlock block, Type type, Value operand) {
        super(block, type, operand);
    }
}
