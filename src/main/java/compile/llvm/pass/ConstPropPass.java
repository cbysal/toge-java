package compile.llvm.pass;

import compile.llvm.BasicBlock;
import compile.llvm.Function;
import compile.llvm.Module;
import compile.llvm.contant.ConstantNumber;
import compile.llvm.ir.*;

import java.util.Objects;

public class ConstPropPass extends FunctionPass {
    public ConstPropPass(Module module) {
        super(module);
    }

    @Override
    public boolean runOnFunction(Function func) {
        boolean modified = false;
        for (BasicBlock block : func) {
            for (int i = 0; i < block.size(); i++) {
                Instruction instruction = block.get(i);
                if (Objects.requireNonNull(instruction) instanceof BinaryOperator) {
                    BinaryOperator binaryOperator = (BinaryOperator) instruction;
                    if (binaryOperator.getOperand(0) instanceof ConstantNumber && binaryOperator.getOperand(1) instanceof ConstantNumber) {
                        ConstantNumber value1 = binaryOperator.getOperand(0);
                        ConstantNumber value2 = binaryOperator.getOperand(1);
                        ConstantNumber newValue;
                        switch (binaryOperator.op) {
                            case ADD:
                            case FADD:
                                newValue = value1.add(value2);
                                break;
                            case SUB:
                            case FSUB:
                                newValue = value1.sub(value2);
                                break;
                            case MUL:
                            case FMUL:
                                newValue = value1.mul(value2);
                                break;
                            case SDIV:
                            case FDIV:
                                newValue = value1.div(value2);
                                break;
                            case SREM:
                                newValue = value1.rem(value2);
                                break;
                            case XOR:
                                newValue = value1.xor(value2);
                                break;
                            default:
                                throw new IllegalArgumentException();
                        }
                        binaryOperator.replaceAllUseAs(newValue);
                        block.remove(i);
                        i--;
                        modified = true;
                    }
                } else if (instruction instanceof ICmpInst) {
                    ICmpInst iCmpInst = (ICmpInst) instruction;
                    if (iCmpInst.getOperand(0) instanceof ConstantNumber && iCmpInst.getOperand(1) instanceof ConstantNumber) {
                        ConstantNumber value1 = iCmpInst.getOperand(0);
                        ConstantNumber value2 = iCmpInst.getOperand(1);
                        ConstantNumber newValue;
                        switch (iCmpInst.getCond()) {
                            case EQ:
                            case OEQ:
                                newValue = value1.eq(value2);
                                break;
                            case NE:
                            case UNE:
                                newValue = value1.ne(value2);
                                break;
                            case SGE:
                            case OGE:
                                newValue = value1.ge(value2);
                                break;
                            case SGT:
                            case OGT:
                                newValue = value1.gt(value2);
                                break;
                            case SLE:
                            case OLE:
                                newValue = value1.le(value2);
                                break;
                            case SLT:
                            case OLT:
                                newValue = value1.lt(value2);
                                break;
                            default:
                                throw new IllegalArgumentException();
                        }
                        iCmpInst.replaceAllUseAs(newValue);
                        block.remove(i);
                        i--;
                        modified = true;
                    }
                }
            }
        }
        return modified;
    }
}
