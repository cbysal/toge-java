package compile.llvm.pass;

import compile.llvm.BasicBlock;
import compile.llvm.Function;
import compile.llvm.Module;
import compile.llvm.contant.ConstantNumber;
import compile.llvm.ir.*;
import compile.llvm.value.Use;

public class ConstPropPass extends FunctionPass {
    public ConstPropPass(Module module) {
        super(module);
    }

    @Override
    public boolean runOnFunction(Function func) {
        boolean modified = false;
        for (BasicBlock block : func) {
            for (int i = 0; i < block.size(); i++) {
                switch (block.get(i)) {
                    case BinaryOperator binaryOperator -> {
                        if (binaryOperator.getOperand(0) instanceof ConstantNumber value1 && binaryOperator.getOperand(1) instanceof ConstantNumber value2) {
                            ConstantNumber newValue = switch (binaryOperator.op) {
                                case ADD, FADD -> value1.add(value2);
                                case SUB, FSUB -> value1.sub(value2);
                                case MUL, FMUL -> value1.mul(value2);
                                case SDIV, FDIV -> value1.div(value2);
                                case SREM -> value1.rem(value2);
                                case XOR -> value1.xor(value2);
                            };
                            binaryOperator.replaceAllUseAs(newValue);
                            block.remove(i);
                            i--;
                            modified = true;
                        }
                    }
                    case ICmpInst iCmpInst -> {
                        if (iCmpInst.getOperand(0) instanceof ConstantNumber value1 && iCmpInst.getOperand(1) instanceof ConstantNumber value2) {
                            ConstantNumber newValue = switch (iCmpInst.getCond()) {
                                case EQ, OEQ -> value1.eq(value2);
                                case NE, UNE -> value1.ne(value2);
                                case SGE, OGE -> value1.ge(value2);
                                case SGT, OGT -> value1.gt(value2);
                                case SLE, OLE -> value1.le(value2);
                                case SLT, OLT -> value1.lt(value2);
                            };
                            iCmpInst.replaceAllUseAs(newValue);
                            block.remove(i);
                            i--;
                            modified = true;
                        }
                    }
                    default -> {
                    }
                }
            }
        }
        return modified;
    }
}
