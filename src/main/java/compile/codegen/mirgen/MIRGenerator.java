package compile.codegen.mirgen;

import compile.codegen.MReg;
import compile.codegen.VReg;
import compile.codegen.mirgen.mir.*;
import compile.codegen.mirgen.trans.MIROpTrans;
import compile.llvm.Module;
import compile.llvm.*;
import compile.llvm.contant.ConstantNumber;
import compile.llvm.ir.*;
import compile.llvm.type.BasicType;
import compile.llvm.value.Value;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class MIRGenerator {
    private final Module module;
    private final Map<String, MachineFunction> mFuncs = new HashMap<>();
    private boolean isProcessed = false;

    public MIRGenerator(Module module) {
        this.module = module;
    }

    private void checkIfIsProcessed() {
        if (isProcessed)
            return;
        isProcessed = true;
        llvm2Mir();
    }

    private Pair<Integer, Integer> getCallerNumbers(Function func) {
        int iSize = 0, fSize = 0;
        for (Argument arg : func.getArgs())
            if (arg.getType() == BasicType.FLOAT)
                fSize = Integer.min(fSize + 1, MReg.F_CALLER_REGS.size());
            else
                iSize = Integer.min(iSize + 1, MReg.I_CALLER_REGS.size());
        return Pair.of(iSize, fSize);
    }

    public Map<String, MachineFunction> getFuncs() {
        checkIfIsProcessed();
        return mFuncs;
    }

    public Set<GlobalVariable> getGlobals() {
        checkIfIsProcessed();
        return new HashSet<>(module.getGlobals());
    }

    private Pair<Integer, Map<AllocaInst, Integer>> calcLocalOffsets(BasicBlock block) {
        int localSize = 0;
        Map<AllocaInst, Integer> localOffsets = new HashMap<>();
        for (Instruction ir : block) {
            if (!(ir instanceof AllocaInst))
                break;
            AllocaInst allocaInst = (AllocaInst) ir;
            int size = allocaInst.getType().baseType().getSize() / 8;
            localOffsets.put(allocaInst, localSize);
            localSize += size;
        }
        return Pair.of(localSize, localOffsets);
    }

    private Map<Argument, Pair<Boolean, Integer>> calcArgOffsets(List<Argument> args) {
        Map<Argument, Pair<Boolean, Integer>> argOffsets = new HashMap<>();
        int iCallerNum = 0, fCallerNum = 0;
        for (Argument arg : args) {
            if (arg.getType() instanceof BasicType && arg.getType() == BasicType.FLOAT)
                fCallerNum++;
            else
                iCallerNum++;
        }
        iCallerNum = Integer.min(iCallerNum, MReg.I_CALLER_REGS.size());
        fCallerNum = Integer.min(fCallerNum, MReg.F_CALLER_REGS.size());
        int iSize = 0, fSize = 0;
        for (Argument arg : args) {
            if (!(arg.getType() instanceof BasicType) || arg.getType() == BasicType.I32) {
                if (iSize < MReg.I_CALLER_REGS.size())
                    argOffsets.put(arg, Pair.of(true, (iCallerNum + fCallerNum - iSize - 1) * 8));
                else
                    argOffsets.put(arg, Pair.of(false, (Integer.max(iSize - MReg.I_CALLER_REGS.size(), 0) + Integer.max(fSize - MReg.F_CALLER_REGS.size(), 0)) * 8));
                iSize++;
            } else {
                if (fSize < MReg.F_CALLER_REGS.size())
                    argOffsets.put(arg, Pair.of(true, (fCallerNum - fSize - 1) * 8));
                else
                    argOffsets.put(arg, Pair.of(false, (Integer.max(iSize - MReg.I_CALLER_REGS.size(), 0) + Integer.max(fSize - MReg.F_CALLER_REGS.size(), 0)) * 8));
                fSize++;
            }
        }
        return argOffsets;
    }

    private void llvm2Mir() {
        for (Function func : module.getFunctions())
            if (!func.isDeclare())
                mFuncs.put(func.getName(), llvm2MirSingle(func));
    }

    private static class FakeMvInst extends Instruction {
        public FakeMvInst(BasicBlock block, Value target, Value source) {
            super(block, BasicType.VOID, target, source);
        }
    }

    private MachineFunction llvm2MirSingle(Function func) {
        Map<Argument, Pair<Boolean, Integer>> argOffsets = calcArgOffsets(func.getArgs());
        Pair<Integer, Map<AllocaInst, Integer>> locals = calcLocalOffsets(func.getFirst());
        Pair<Integer, Integer> callerNums = getCallerNumbers(func);
        MachineFunction mFunc = new MachineFunction(func, locals.getLeft(), callerNums.getLeft(), callerNums.getRight());
        LabelMIR retLabelMIR = new LabelMIR(new BasicBlock(func));
        Map<VReg, MReg> replaceMap = new HashMap<>();
        Map<Instruction, VReg> instRegMap = new HashMap<>();
        for (BasicBlock block : func) {
            for (Instruction inst : block) {
                instRegMap.put(inst, new VReg(inst.getType() == BasicType.FLOAT ? BasicType.FLOAT : BasicType.I32));
            }
        }
        for (BasicBlock block : func) {
            for (int i = 0; i < block.size(); i++) {
                Instruction inst = block.get(i);
                if (inst instanceof PHINode) {
                    PHINode phiNode = (PHINode) inst;
                    for (int j = 0; j < phiNode.size(); j++) {
                        Pair<BasicBlock, Value> blockValue = phiNode.getBlockValue(j);
                        BasicBlock block2 = blockValue.getLeft();
                        Value value = blockValue.getRight();
                        block2.add(block2.size() - 1, new FakeMvInst(block2, phiNode, value));
                    }
                    block.remove(i);
                    i--;
                }
            }
        }
        Map<AllocaInst, Integer> localOffsets = locals.getRight();
        for (BasicBlock block : func) {
            mFunc.addIR(new LabelMIR(block));
            for (Instruction inst : block) {
                if (inst instanceof BinaryOperator) {
                    BinaryOperator binaryOperator = (BinaryOperator) inst;
                    MIROpTrans.transBinary(mFunc.getIrs(), instRegMap, argOffsets, binaryOperator);
                    continue;
                }
                if (inst instanceof BranchInst) {
                    BranchInst branchInst = (BranchInst) inst;
                    MIROpTrans.transBranch(mFunc.getIrs(), instRegMap, branchInst);
                    continue;
                }
                if (inst instanceof CallInst) {
                    CallInst callInst = (CallInst) inst;
                    int paramNum = MIROpTrans.transCall(mFunc.getIrs(), instRegMap, argOffsets, callInst, localOffsets);
                    mFunc.setMaxFuncParamNum(Integer.max(mFunc.getMaxFuncParamNum(), paramNum));
                    continue;
                }
                if (inst instanceof GetElementPtrInst) {
                    GetElementPtrInst getElementPtrInst = (GetElementPtrInst) inst;
                    Value pointer = getElementPtrInst.getOperand(0);
                    if (pointer instanceof GlobalVariable) {
                        GlobalVariable global = (GlobalVariable) pointer;
                        VReg midReg1 = new VReg(BasicType.I32);
                        VReg midReg2 = new VReg(BasicType.I32);
                        VReg midReg3 = new VReg(BasicType.I32);
                        VReg midReg4 = new VReg(BasicType.I32);
                        mFunc.getIrs().add(new LlaMIR(midReg1, global));
                        mFunc.getIrs().add(new LiMIR(midReg2, getElementPtrInst.getType().baseType().getSize() / 8));
                        Value lastOperand = getElementPtrInst.getLastOperand();
                        if (Objects.requireNonNull(lastOperand) instanceof Argument) {
                            Argument arg = (Argument) lastOperand;
                            mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg3, argOffsets.get(arg).getRight()));
                        } else if (lastOperand instanceof Instruction) {
                            Instruction indexInst = (Instruction) lastOperand;
                            mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg3, instRegMap.get(indexInst)));
                        } else if (lastOperand instanceof ConstantNumber) {
                            ConstantNumber value = (ConstantNumber) lastOperand;
                            if (value.getType() == BasicType.FLOAT) {
                                VReg midReg = new VReg(BasicType.I32);
                                mFunc.getIrs().add(new LiMIR(midReg, Float.floatToIntBits(value.floatValue())));
                                mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg3, midReg));
                            } else
                                mFunc.getIrs().add(new LiMIR(midReg3, value.intValue()));
                        } else {
                            throw new IllegalStateException("Unexpected value: " + getElementPtrInst.getLastOperand());
                        }
                        mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.MUL, midReg4, midReg2, midReg3));
                        mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.ADD, instRegMap.get(getElementPtrInst), midReg1, midReg4));
                        continue;
                    }
                    if (pointer instanceof Argument) {
                        Argument arg = (Argument) pointer;
                        Pair<Boolean, Integer> innerOffset = argOffsets.get(arg);
                        VReg midReg1 = new VReg(BasicType.I32);
                        VReg midReg2 = new VReg(BasicType.I32);
                        VReg midReg3 = new VReg(BasicType.I32);
                        VReg midReg4 = new VReg(BasicType.I32);
                        mFunc.getIrs().add(new LoadItemMIR(innerOffset.getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg1, innerOffset.getRight()));
                        mFunc.getIrs().add(new LiMIR(midReg2, getElementPtrInst.getType().baseType().getSize() / 8));
                        Value lastOperand = getElementPtrInst.getLastOperand();
                        if (Objects.requireNonNull(lastOperand) instanceof Argument) {
                            Argument indexArg = (Argument) lastOperand;
                            mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg3, argOffsets.get(indexArg).getRight()));
                        } else if (lastOperand instanceof Instruction) {
                            Instruction indexInst = (Instruction) lastOperand;
                            mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg3, instRegMap.get(indexInst)));
                        } else if (lastOperand instanceof ConstantNumber) {
                            ConstantNumber value = (ConstantNumber) lastOperand;
                            if (value.getType() == BasicType.FLOAT) {
                                VReg midReg = new VReg(BasicType.I32);
                                mFunc.getIrs().add(new LiMIR(midReg, Float.floatToIntBits(value.floatValue())));
                                mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg3, midReg));
                            } else
                                mFunc.getIrs().add(new LiMIR(midReg3, value.intValue()));
                        } else {
                            throw new IllegalStateException("Unexpected value: " + getElementPtrInst.getLastOperand());
                        }
                        mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.MUL, midReg4, midReg2, midReg3));
                        mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.ADD, instRegMap.get(getElementPtrInst), midReg1, midReg4));
                    }
                    if (pointer instanceof AllocaInst) {
                        AllocaInst allocaInst = (AllocaInst) pointer;
                        if (getElementPtrInst.size() == 3) {
                            VReg midReg1 = new VReg(BasicType.I32);
                            VReg midReg2 = new VReg(BasicType.I32);
                            VReg midReg3 = new VReg(BasicType.I32);
                            VReg midReg4 = new VReg(BasicType.I32);
                            mFunc.getIrs().add(new AddRegLocalMIR(midReg1, localOffsets.get(allocaInst)));
                            Value lastOperand = getElementPtrInst.getLastOperand();
                            if (Objects.requireNonNull(lastOperand) instanceof Argument) {
                                Argument arg = (Argument) lastOperand;
                                mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg2, argOffsets.get(arg).getRight()));
                            } else if (lastOperand instanceof Instruction) {
                                Instruction indexInst = (Instruction) lastOperand;
                                mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg2, instRegMap.get(indexInst)));
                            } else if (lastOperand instanceof ConstantNumber) {
                                ConstantNumber value = (ConstantNumber) lastOperand;
                                if (value.getType() == BasicType.FLOAT) {
                                    VReg midReg = new VReg(BasicType.I32);
                                    mFunc.getIrs().add(new LiMIR(midReg, Float.floatToIntBits(value.floatValue())));
                                    mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg2, midReg));
                                } else
                                    mFunc.getIrs().add(new LiMIR(midReg2, value.intValue()));
                            } else {
                                throw new IllegalStateException("Unexpected value: " + getElementPtrInst.getLastOperand());
                            }
                            mFunc.getIrs().add(new LiMIR(midReg3, pointer.getType().baseType().baseType().getSize() / 8));
                            mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.MUL, midReg4, midReg2, midReg3));
                            mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.ADD, instRegMap.get(getElementPtrInst), midReg1, midReg4));
                        } else {
                            VReg midReg1 = new VReg(BasicType.I32);
                            VReg midReg2 = new VReg(BasicType.I32);
                            VReg midReg3 = new VReg(BasicType.I32);
                            VReg midReg4 = new VReg(BasicType.I32);
                            mFunc.getIrs().add(new AddRegLocalMIR(midReg1, localOffsets.get(allocaInst)));
                            Value lastOperand = getElementPtrInst.getLastOperand();
                            if (Objects.requireNonNull(lastOperand) instanceof Argument) {
                                Argument arg = (Argument) lastOperand;
                                mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg2, argOffsets.get(arg).getRight()));
                            } else if (lastOperand instanceof Instruction) {
                                Instruction indexInst = (Instruction) lastOperand;
                                mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg2, instRegMap.get(indexInst)));
                            } else if (lastOperand instanceof ConstantNumber) {
                                ConstantNumber value = (ConstantNumber) lastOperand;
                                if (value.getType() == BasicType.FLOAT) {
                                    VReg midReg = new VReg(BasicType.I32);
                                    mFunc.getIrs().add(new LiMIR(midReg, Float.floatToIntBits(value.floatValue())));
                                    mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg2, midReg));
                                } else
                                    mFunc.getIrs().add(new LiMIR(midReg2, value.intValue()));
                            } else {
                                throw new IllegalStateException("Unexpected value: " + getElementPtrInst.getLastOperand());
                            }
                            mFunc.getIrs().add(new LiMIR(midReg3, pointer.getType().baseType().getSize() / 8));
                            mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.MUL, midReg4, midReg2, midReg3));
                            mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.ADD, instRegMap.get(getElementPtrInst), midReg1, midReg4));
                        }
                        continue;
                    }
                    if (pointer instanceof Instruction) {
                        Instruction pointerInst = (Instruction) pointer;
                        if (getElementPtrInst.size() == 3) {
                            VReg midReg1 = new VReg(BasicType.I32);
                            VReg midReg2 = new VReg(BasicType.I32);
                            VReg midReg3 = new VReg(BasicType.I32);
                            Value lastOperand = getElementPtrInst.getLastOperand();
                            if (Objects.requireNonNull(lastOperand) instanceof Argument) {
                                Argument arg = (Argument) lastOperand;
                                mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg1, argOffsets.get(arg).getRight()));
                            } else if (lastOperand instanceof Instruction) {
                                Instruction indexInst = (Instruction) lastOperand;
                                mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg1, instRegMap.get(indexInst)));
                            } else if (lastOperand instanceof ConstantNumber) {
                                ConstantNumber value = (ConstantNumber) lastOperand;
                                if (value.getType() == BasicType.FLOAT) {
                                    VReg midReg = new VReg(BasicType.I32);
                                    mFunc.getIrs().add(new LiMIR(midReg, Float.floatToIntBits(value.floatValue())));
                                    mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg1, midReg));
                                } else
                                    mFunc.getIrs().add(new LiMIR(midReg1, value.intValue()));
                            } else {
                                throw new IllegalStateException("Unexpected value: " + getElementPtrInst.getLastOperand());
                            }
                            mFunc.getIrs().add(new LiMIR(midReg2, pointer.getType().baseType().baseType().getSize() / 8));
                            mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.MUL, midReg3, midReg1, midReg2));
                            mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.ADD, instRegMap.get(getElementPtrInst), instRegMap.get(pointerInst), midReg3));
                        } else {
                            VReg midReg1 = new VReg(BasicType.I32);
                            VReg midReg2 = new VReg(BasicType.I32);
                            VReg midReg3 = new VReg(BasicType.I32);
                            Value lastOperand = getElementPtrInst.getLastOperand();
                            if (Objects.requireNonNull(lastOperand) instanceof Argument) {
                                Argument arg = (Argument) lastOperand;
                                mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg1, argOffsets.get(arg).getRight()));
                            } else if (lastOperand instanceof Instruction) {
                                Instruction indexInst = (Instruction) lastOperand;
                                mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg1, instRegMap.get(indexInst)));
                            } else if (lastOperand instanceof ConstantNumber) {
                                ConstantNumber value = (ConstantNumber) lastOperand;
                                if (value.getType() == BasicType.FLOAT) {
                                    VReg midReg = new VReg(BasicType.I32);
                                    mFunc.getIrs().add(new LiMIR(midReg, Float.floatToIntBits(value.floatValue())));
                                    mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg1, midReg));
                                } else
                                    mFunc.getIrs().add(new LiMIR(midReg1, value.intValue()));
                            } else {
                                throw new IllegalStateException("Unexpected value: " + getElementPtrInst.getLastOperand());
                            }
                            mFunc.getIrs().add(new LiMIR(midReg2, pointer.getType().baseType().getSize() / 8));
                            mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.MUL, midReg3, midReg1, midReg2));
                            mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.ADD, instRegMap.get(getElementPtrInst), instRegMap.get(pointerInst), midReg3));
                        }
                    }
                    continue;
                }
                if (inst instanceof LoadInst) {
                    LoadInst loadInst = (LoadInst) inst;
                    Value pointer = loadInst.getOperand(0);
                    if (pointer instanceof GlobalVariable) {
                        GlobalVariable global = (GlobalVariable) pointer;
                        VReg midReg = new VReg(BasicType.I32);
                        mFunc.getIrs().add(new LlaMIR(midReg, global));
                        mFunc.getIrs().add(new LoadMIR(instRegMap.get(loadInst), midReg, 0, 4));
                        continue;
                    }
                    if (pointer instanceof Argument) {
                        Argument arg = (Argument) pointer;
                        Pair<Boolean, Integer> innerOffset = argOffsets.get(arg);
                        mFunc.getIrs().add(new LoadItemMIR(innerOffset.getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, instRegMap.get(loadInst), innerOffset.getRight()));
                    }
                    if (pointer instanceof AllocaInst) {
                        AllocaInst allocaInst = (AllocaInst) pointer;
                        VReg midReg = new VReg(BasicType.I32);
                        mFunc.getIrs().add(new AddRegLocalMIR(midReg, localOffsets.get(allocaInst)));
                        mFunc.getIrs().add(new LoadMIR(instRegMap.get(loadInst), midReg, 0, allocaInst.getType().baseType().getSize() / 8));
                        continue;
                    }
                    if (pointer instanceof Instruction) {
                        Instruction pointerInst = (Instruction) pointer;
                        mFunc.getIrs().add(new LoadMIR(instRegMap.get(loadInst), instRegMap.get(pointerInst), 0, pointerInst.getType().baseType().getSize() / 8));
                    }
                    continue;
                }
                if (inst instanceof RetInst) {
                    RetInst retInst = (RetInst) inst;
                    if (!retInst.isEmpty()) {
                        Value retVal = retInst.getOperand(0);
                        if (Objects.requireNonNull(retVal) instanceof Argument) {
                            Argument arg = (Argument) retVal;
                            mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, retVal.getType() == BasicType.I32 ? MReg.A0 : MReg.FA0, argOffsets.get(arg).getRight()));
                        } else if (retVal instanceof Instruction) {
                            Instruction valueInst = (Instruction) retVal;
                            mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, retVal.getType() == BasicType.I32 ? MReg.A0 : MReg.FA0, instRegMap.get(valueInst)));
                        } else if (retVal instanceof ConstantNumber) {
                            ConstantNumber value = (ConstantNumber) retVal;
                            if (value.getType().equals(BasicType.I32)) {
                                mFunc.getIrs().add(new LiMIR(MReg.A0, value.intValue()));
                            } else if (value.getType().equals(BasicType.FLOAT)) {
                                VReg midReg = new VReg(BasicType.I32);
                                mFunc.getIrs().add(new LiMIR(midReg, Float.floatToIntBits(value.floatValue())));
                                mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, MReg.FA0, midReg));
                            } else {
                                throw new IllegalStateException("Unexpected value: " + value.getType());
                            }
                        } else {
                            throw new IllegalStateException("Unexpected value: " + retVal);
                        }
                    }
                    mFunc.getIrs().add(new BMIR(null, null, null, retLabelMIR.getBlock()));
                    continue;
                }
                if (inst instanceof StoreInst) {
                    StoreInst storeInst = (StoreInst) inst;
                    Value value = storeInst.getOperand(0);
                    Value pointer = storeInst.getOperand(1);
                    if (pointer instanceof GlobalVariable) {
                        GlobalVariable global = (GlobalVariable) pointer;
                        VReg midReg1 = new VReg(BasicType.I32);
                        VReg midReg2 = new VReg(BasicType.I32);
                        mFunc.getIrs().add(new LlaMIR(midReg1, global));
                        if (Objects.requireNonNull(value) instanceof Argument) {
                            Argument arg = (Argument) value;
                            mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg2, argOffsets.get(arg).getRight()));
                        } else if (value instanceof Instruction) {
                            Instruction valueInst = (Instruction) value;
                            mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg2, instRegMap.get(valueInst)));
                        } else if (value instanceof ConstantNumber) {
                            ConstantNumber v = (ConstantNumber) value;
                            if (v.getType() == BasicType.FLOAT) {
                                VReg midReg = new VReg(BasicType.I32);
                                mFunc.getIrs().add(new LiMIR(midReg, Float.floatToIntBits(v.floatValue())));
                                mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg2, midReg));
                            } else
                                mFunc.getIrs().add(new LiMIR(midReg2, v.intValue()));
                        } else {
                            throw new IllegalStateException("Unexpected value: " + value);
                        }
                        mFunc.getIrs().add(new StoreMIR(midReg2, midReg1, 0, 4));
                        continue;
                    }
                    if (pointer instanceof Argument) {
                        Argument arg = (Argument) pointer;
                        Pair<Boolean, Integer> innerOffset = argOffsets.get(arg);
                        VReg midReg1 = new VReg(BasicType.I32);
                        VReg midReg2 = new VReg(BasicType.I32);
                        mFunc.getIrs().add(new LoadItemMIR(innerOffset.getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg1, innerOffset.getRight()));
                        if (Objects.requireNonNull(value) instanceof Argument) {
                            Argument valueArg = (Argument) value;
                            mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg2, argOffsets.get(valueArg).getRight()));
                        } else if (value instanceof Instruction) {
                            Instruction valueInst = (Instruction) value;
                            mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg2, instRegMap.get(valueInst)));
                        } else if (value instanceof ConstantNumber) {
                            ConstantNumber v = (ConstantNumber) value;
                            if (v.getType() == BasicType.FLOAT) {
                                VReg midReg = new VReg(BasicType.I32);
                                mFunc.getIrs().add(new LiMIR(midReg, Float.floatToIntBits(v.floatValue())));
                                mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg2, midReg));
                            } else
                                mFunc.getIrs().add(new LiMIR(midReg2, v.intValue()));
                        } else {
                            throw new IllegalStateException("Unexpected value: " + value);
                        }
                        mFunc.getIrs().add(new StoreMIR(midReg2, midReg1, 0, 4));
                    }
                    if (pointer instanceof AllocaInst) {
                        AllocaInst allocaInst = (AllocaInst) pointer;
                        VReg midReg1 = new VReg(BasicType.I32);
                        VReg midReg2 = new VReg(BasicType.I32);
                        if (Objects.requireNonNull(value) instanceof Argument) {
                            Argument arg = (Argument) value;
                            mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg1, argOffsets.get(arg).getRight()));
                        } else if (value instanceof Instruction) {
                            Instruction valueInst = (Instruction) value;
                            mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg1, instRegMap.get(valueInst)));
                        } else if (value instanceof ConstantNumber) {
                            ConstantNumber v = (ConstantNumber) value;
                            if (v.getType() == BasicType.FLOAT) {
                                VReg midReg = new VReg(BasicType.I32);
                                mFunc.getIrs().add(new LiMIR(midReg, Float.floatToIntBits(v.floatValue())));
                                mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg1, midReg));
                            } else
                                mFunc.getIrs().add(new LiMIR(midReg1, v.intValue()));
                        } else {
                            throw new IllegalStateException("Unexpected value: " + value);
                        }
                        mFunc.getIrs().add(new AddRegLocalMIR(midReg2, localOffsets.get(allocaInst)));
                        mFunc.getIrs().add(new StoreMIR(midReg1, midReg2, 0, allocaInst.getType().baseType().getSize() / 8));
                        continue;
                    }
                    if (pointer instanceof Instruction) {
                        Instruction pointerInst = (Instruction) pointer;
                        VReg midReg = new VReg(BasicType.I32);
                        if (Objects.requireNonNull(value) instanceof Argument) {
                            Argument arg = (Argument) value;
                            mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg, argOffsets.get(arg).getRight()));
                        } else if (value instanceof Instruction) {
                            Instruction valueInst = (Instruction) value;
                            mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg, instRegMap.get(valueInst)));
                        } else if (value instanceof ConstantNumber) {
                            ConstantNumber v = (ConstantNumber) value;
                            if (v.getType() == BasicType.FLOAT) {
                                VReg midReg1 = new VReg(BasicType.I32);
                                mFunc.getIrs().add(new LiMIR(midReg1, Float.floatToIntBits(v.floatValue())));
                                mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg, midReg1));
                            } else
                                mFunc.getIrs().add(new LiMIR(midReg, v.intValue()));
                        } else {
                            throw new IllegalStateException("Unexpected value: " + value);
                        }
                        mFunc.getIrs().add(new StoreMIR(midReg, instRegMap.get(pointerInst), 0, pointerInst.getType().baseType().getSize() / 8));
                    }
                    continue;
                }
                if (inst instanceof AllocaInst) {
                    continue;
                }
                if (inst instanceof BitCastInst) {
                    BitCastInst bitCastInst = (BitCastInst) inst;
                    Instruction operand = bitCastInst.getOperand(0);
                    VReg srcReg = instRegMap.get(operand);
                    if (operand instanceof AllocaInst) {
                        AllocaInst allocaInst = (AllocaInst) operand;
                        srcReg = new VReg(BasicType.I32);
                        mFunc.getIrs().add(new AddRegLocalMIR(srcReg, localOffsets.get(allocaInst)));
                    }
                    mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, instRegMap.get(bitCastInst), srcReg));
                    continue;
                }
                if (inst instanceof ICmpInst) {
                    ICmpInst iCmpInst = (ICmpInst) inst;
                    VReg target = instRegMap.get(iCmpInst);
                    VReg operand1;
                    Value operand = iCmpInst.getOperand(0);
                    if (Objects.requireNonNull(operand) instanceof Argument) {
                        Argument arg = (Argument) operand;
                        VReg midReg = new VReg(arg.getType() == BasicType.FLOAT ? BasicType.FLOAT : BasicType.I32);
                        mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg, argOffsets.get(arg).getRight()));
                        operand1 = midReg;
                    } else if (operand instanceof Instruction) {
                        Instruction valueInst = (Instruction) operand;
                        operand1 = instRegMap.get(valueInst);
                    } else if (operand instanceof ConstantNumber) {
                        ConstantNumber value = (ConstantNumber) operand;
                        VReg midReg = new VReg(value.getType());
                        mFunc.getIrs().add(new LiMIR(midReg, value.intValue()));
                        operand1 = midReg;
                    } else {
                        throw new IllegalStateException("Unexpected value: " + iCmpInst.getOperand(0));
                    }
                    VReg operand2;
                    Value iCmpInstOperand = iCmpInst.getOperand(1);
                    if (Objects.requireNonNull(iCmpInstOperand) instanceof Argument) {
                        Argument arg = (Argument) iCmpInstOperand;
                        VReg midReg = new VReg(arg.getType() == BasicType.FLOAT ? BasicType.FLOAT : BasicType.I32);
                        mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg, argOffsets.get(arg).getRight()));
                        operand2 = midReg;
                    } else if (iCmpInstOperand instanceof Instruction) {
                        Instruction valueInst = (Instruction) iCmpInstOperand;
                        operand2 = instRegMap.get(valueInst);
                    } else if (iCmpInstOperand instanceof ConstantNumber) {
                        ConstantNumber value = (ConstantNumber) iCmpInstOperand;
                        VReg midReg = new VReg(value.getType());
                        mFunc.getIrs().add(new LiMIR(midReg, value.intValue()));
                        operand2 = midReg;
                    } else {
                        throw new IllegalStateException("Unexpected value: " + iCmpInst.getOperand(1));
                    }
                    switch (iCmpInst.getCond()) {
                        case EQ: {
                            VReg midReg = new VReg(BasicType.I32);
                            mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.SUB, midReg, operand1, operand2));
                            mFunc.getIrs().add(new RrMIR(RrMIR.Op.SEQZ, target, midReg));
                            break;
                        }
                        case NE: {
                            VReg midReg = new VReg(BasicType.I32);
                            mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.SUB, midReg, operand1, operand2));
                            mFunc.getIrs().add(new RrMIR(RrMIR.Op.SNEZ, target, midReg));
                            break;
                        }
                        case SGE: {
                            VReg midReg = new VReg(BasicType.I32);
                            mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.SLT, midReg, operand1, operand2));
                            mFunc.getIrs().add(new RriMIR(RriMIR.Op.XORI, target, midReg, 1));
                            break;
                        }
                        case SGT:
                            mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.SGT, target, operand1, operand2));
                            break;
                        case SLE: {
                            VReg midReg = new VReg(BasicType.I32);
                            mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.SGT, midReg, operand1, operand2));
                            mFunc.getIrs().add(new RriMIR(RriMIR.Op.XORI, target, midReg, 1));
                            break;
                        }
                        case SLT:
                            mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.SLT, target, operand1, operand2));
                            break;
                    }
                    continue;
                }
                if (inst instanceof FCmpInst) {
                    FCmpInst fCmpInst = (FCmpInst) inst;
                    VReg target = instRegMap.get(fCmpInst);
                    VReg operand1;
                    Value operand = fCmpInst.getOperand(0);
                    if (Objects.requireNonNull(operand) instanceof Argument) {
                        Argument arg = (Argument) operand;
                        VReg midReg = new VReg(arg.getType() == BasicType.FLOAT ? BasicType.FLOAT : BasicType.I32);
                        mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg, argOffsets.get(arg).getRight()));
                        operand1 = midReg;
                    } else if (operand instanceof Instruction) {
                        Instruction valueInst = (Instruction) operand;
                        operand1 = instRegMap.get(valueInst);
                    } else if (operand instanceof ConstantNumber) {
                        ConstantNumber value = (ConstantNumber) operand;
                        VReg midReg1 = new VReg(BasicType.I32);
                        VReg midReg2 = new VReg(BasicType.FLOAT);
                        mFunc.getIrs().add(new LiMIR(midReg1, Float.floatToIntBits(value.floatValue())));
                        mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg2, midReg1));
                        operand1 = midReg2;
                    } else {
                        throw new IllegalStateException("Unexpected value: " + fCmpInst.getOperand(0));
                    }
                    VReg operand2;
                    Value fCmpInstOperand = fCmpInst.getOperand(1);
                    if (Objects.requireNonNull(fCmpInstOperand) instanceof Argument) {
                        Argument arg = (Argument) fCmpInstOperand;
                        VReg midReg = new VReg(arg.getType() == BasicType.FLOAT ? BasicType.FLOAT : BasicType.I32);
                        mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg, argOffsets.get(arg).getRight()));
                        operand2 = midReg;
                    } else if (fCmpInstOperand instanceof Instruction) {
                        Instruction valueInst = (Instruction) fCmpInstOperand;
                        operand2 = instRegMap.get(valueInst);
                    } else if (fCmpInstOperand instanceof ConstantNumber) {
                        ConstantNumber value = (ConstantNumber) fCmpInstOperand;
                        VReg midReg1 = new VReg(BasicType.I32);
                        VReg midReg2 = new VReg(BasicType.FLOAT);
                        mFunc.getIrs().add(new LiMIR(midReg1, Float.floatToIntBits(value.floatValue())));
                        mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg2, midReg1));
                        operand2 = midReg2;
                    } else {
                        throw new IllegalStateException("Unexpected value: " + fCmpInst.getOperand(1));
                    }
                    if (fCmpInst.getCond() == FCmpInst.Cond.UNE) {
                        mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.EQ, target, operand1, operand2));
                        mFunc.getIrs().add(new RriMIR(RriMIR.Op.XORI, target, target, 1));
                        continue;
                    }
                    RrrMIR.Op op;
                    switch (fCmpInst.getCond()) {
                        case OEQ:
                            op = RrrMIR.Op.EQ;
                            break;
                        case OGE:
                            op = RrrMIR.Op.GE;
                            break;
                        case OGT:
                            op = RrrMIR.Op.GT;
                            break;
                        case OLE:
                            op = RrrMIR.Op.LE;
                            break;
                        case OLT:
                            op = RrrMIR.Op.LT;
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + fCmpInst.getCond());
                    }
                    mFunc.getIrs().add(new RrrMIR(op, target, operand1, operand2));
                    continue;
                }
                if (inst instanceof ZExtInst) {
                    ZExtInst zExtInst = (ZExtInst) inst;
                    VReg operand;
                    Value zExtInstOperand = zExtInst.getOperand(0);
                    if (Objects.requireNonNull(zExtInstOperand) instanceof Argument) {
                        Argument arg = (Argument) zExtInstOperand;
                        VReg midReg = new VReg(arg.getType() == BasicType.FLOAT ? BasicType.FLOAT : BasicType.I32);
                        mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg, argOffsets.get(arg).getRight()));
                        operand = midReg;
                    } else if (zExtInstOperand instanceof Instruction) {
                        Instruction valueInst = (Instruction) zExtInstOperand;
                        operand = instRegMap.get(valueInst);
                    } else if (zExtInstOperand instanceof ConstantNumber) {
                        ConstantNumber value = (ConstantNumber) zExtInstOperand;
                        VReg midReg = new VReg(value.getType());
                        mFunc.getIrs().add(new LiMIR(midReg, value.intValue()));
                        operand = midReg;
                    } else {
                        throw new IllegalStateException("Unexpected value: " + zExtInst.getOperand(0));
                    }
                    mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, instRegMap.get(zExtInst), operand));
                    continue;
                }
                if (inst instanceof SExtInst) {
                    SExtInst sExtInst = (SExtInst) inst;
                    VReg operand;
                    Value sExtInstOperand = sExtInst.getOperand(0);
                    if (Objects.requireNonNull(sExtInstOperand) instanceof Argument) {
                        Argument arg = (Argument) sExtInstOperand;
                        VReg midReg = new VReg(arg.getType() == BasicType.FLOAT ? BasicType.FLOAT : BasicType.I32);
                        mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg, argOffsets.get(arg).getRight()));
                        operand = midReg;
                    } else if (sExtInstOperand instanceof Instruction) {
                        Instruction valueInst = (Instruction) sExtInstOperand;
                        operand = instRegMap.get(valueInst);
                    } else if (sExtInstOperand instanceof ConstantNumber) {
                        ConstantNumber value = (ConstantNumber) sExtInstOperand;
                        VReg midReg = new VReg(value.getType());
                        mFunc.getIrs().add(new LiMIR(midReg, value.intValue()));
                        operand = midReg;
                    } else {
                        throw new IllegalStateException("Unexpected value: " + sExtInst.getOperand(0));
                    }
                    mFunc.getIrs().add(new RrMIR(RrMIR.Op.NEG, instRegMap.get(sExtInst), operand));
                    continue;
                }
                if (inst instanceof FPToSIInst) {
                    FPToSIInst fpToSIInst = (FPToSIInst) inst;
                    VReg operand;
                    Value fpToSIInstOperand = fpToSIInst.getOperand(0);
                    if (Objects.requireNonNull(fpToSIInstOperand) instanceof Argument) {
                        Argument arg = (Argument) fpToSIInstOperand;
                        VReg midReg = new VReg(arg.getType() == BasicType.FLOAT ? BasicType.FLOAT : BasicType.I32);
                        mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg, argOffsets.get(arg).getRight()));
                        operand = midReg;
                    } else if (fpToSIInstOperand instanceof Instruction) {
                        Instruction valueInst = (Instruction) fpToSIInstOperand;
                        operand = instRegMap.get(valueInst);
                    } else if (fpToSIInstOperand instanceof ConstantNumber) {
                        ConstantNumber value = (ConstantNumber) fpToSIInstOperand;
                        VReg midReg1 = new VReg(BasicType.I32);
                        VReg midReg2 = new VReg(BasicType.FLOAT);
                        mFunc.getIrs().add(new LiMIR(midReg1, Float.floatToIntBits(value.floatValue())));
                        mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg2, midReg1));
                        operand = midReg2;
                    } else {
                        throw new IllegalStateException("Unexpected value: " + fpToSIInst.getOperand(0));
                    }
                    mFunc.getIrs().add(new RrMIR(RrMIR.Op.CVT, instRegMap.get(fpToSIInst), operand));
                    continue;
                }
                if (inst instanceof SIToFPInst) {
                    SIToFPInst siToFPInst = (SIToFPInst) inst;
                    VReg operand;
                    Value siToFPInstOperand = siToFPInst.getOperand(0);
                    if (Objects.requireNonNull(siToFPInstOperand) instanceof Argument) {
                        Argument arg = (Argument) siToFPInstOperand;
                        VReg midReg = new VReg(arg.getType() == BasicType.FLOAT ? BasicType.FLOAT : BasicType.I32);
                        mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg, argOffsets.get(arg).getRight()));
                        operand = midReg;
                    } else if (siToFPInstOperand instanceof Instruction) {
                        Instruction valueInst = (Instruction) siToFPInstOperand;
                        operand = instRegMap.get(valueInst);
                    } else if (siToFPInstOperand instanceof ConstantNumber) {
                        ConstantNumber value = (ConstantNumber) siToFPInstOperand;
                        VReg midReg = new VReg(value.getType());
                        mFunc.getIrs().add(new LiMIR(midReg, value.intValue()));
                        operand = midReg;
                    } else {
                        throw new IllegalStateException("Unexpected value: " + siToFPInst.getOperand(0));
                    }
                    mFunc.getIrs().add(new RrMIR(RrMIR.Op.CVT, instRegMap.get(siToFPInst), operand));
                    continue;
                }
                if (inst instanceof FakeMvInst) {
                    FakeMvInst fakeMvInst = (FakeMvInst) inst;
                    PHINode phiNode = fakeMvInst.getOperand(0);
                    VReg target = instRegMap.get(phiNode);
                    VReg source;
                    Value operand = fakeMvInst.getOperand(1);
                    if (Objects.requireNonNull(operand) instanceof Argument) {
                        Argument arg = (Argument) operand;
                        VReg midReg = new VReg(arg.getType() == BasicType.FLOAT ? BasicType.FLOAT : BasicType.I32);
                        mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg, argOffsets.get(arg).getRight()));
                        source = midReg;
                    } else if (operand instanceof Instruction) {
                        Instruction inst1 = (Instruction) operand;
                        source = instRegMap.get(inst1);
                    } else if (operand instanceof ConstantNumber) {
                        ConstantNumber value = (ConstantNumber) operand;
                        VReg midReg = new VReg(value.getType());
                        if (midReg.getType().equals(BasicType.I32)) {
                            mFunc.getIrs().add(new LiMIR(midReg, value.intValue()));
                        } else if (midReg.getType().equals(BasicType.FLOAT)) {
                            VReg midReg1 = new VReg(BasicType.I32);
                            mFunc.getIrs().add(new LiMIR(midReg1, Float.floatToIntBits(value.floatValue())));
                            mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg, midReg1));
                        } else {
                            throw new IllegalStateException("Unexpected value: " + midReg.getType());
                        }
                        source = midReg;
                    } else {
                        source = null;
                    }
                    mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, target, source));
                    continue;
                }
                throw new IllegalStateException("Unexpected value: " + inst);
            }
        }
        mFunc.getIrs().add(retLabelMIR);
        mFunc.getIrs().replaceAll(ir -> ir.replaceReg(replaceMap));
        return mFunc;
    }
}
