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
            if (!(ir instanceof AllocaInst allocaInst))
                break;
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
                if (inst instanceof PHINode phiNode) {
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
                if (inst instanceof BinaryOperator binaryOperator) {
                    MIROpTrans.transBinary(mFunc.getIrs(), instRegMap, argOffsets, binaryOperator);
                    continue;
                }
                if (inst instanceof BranchInst branchInst) {
                    MIROpTrans.transBranch(mFunc.getIrs(), instRegMap, branchInst);
                    continue;
                }
                if (inst instanceof CallInst callInst) {
                    int paramNum = MIROpTrans.transCall(mFunc.getIrs(), instRegMap, argOffsets, callInst, localOffsets);
                    mFunc.setMaxFuncParamNum(Integer.max(mFunc.getMaxFuncParamNum(), paramNum));
                    continue;
                }
                if (inst instanceof GetElementPtrInst getElementPtrInst) {
                    Value pointer = getElementPtrInst.getOperand(0);
                    if (pointer instanceof GlobalVariable global) {
                        VReg midReg1 = new VReg(BasicType.I32);
                        VReg midReg2 = new VReg(BasicType.I32);
                        VReg midReg3 = new VReg(BasicType.I32);
                        VReg midReg4 = new VReg(BasicType.I32);
                        mFunc.getIrs().add(new LlaMIR(midReg1, global));
                        mFunc.getIrs().add(new LiMIR(midReg2, getElementPtrInst.getType().baseType().getSize() / 8));
                        switch (getElementPtrInst.getLastOperand()) {
                            case Argument arg ->
                                    mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg3, argOffsets.get(arg).getRight()));
                            case Instruction indexInst ->
                                    mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg3, instRegMap.get(indexInst)));
                            case ConstantNumber value -> {
                                if (value.getType() == BasicType.FLOAT) {
                                    VReg midReg = new VReg(BasicType.I32);
                                    mFunc.getIrs().add(new LiMIR(midReg, Float.floatToIntBits(value.floatValue())));
                                    mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg3, midReg));
                                } else
                                    mFunc.getIrs().add(new LiMIR(midReg3, value.intValue()));
                            }
                            default ->
                                    throw new IllegalStateException("Unexpected value: " + getElementPtrInst.getLastOperand());
                        }
                        mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.MUL, midReg4, midReg2, midReg3));
                        mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.ADD, instRegMap.get(getElementPtrInst), midReg1, midReg4));
                        continue;
                    }
                    if (pointer instanceof Argument arg) {
                        Pair<Boolean, Integer> innerOffset = argOffsets.get(arg);
                        VReg midReg1 = new VReg(BasicType.I32);
                        VReg midReg2 = new VReg(BasicType.I32);
                        VReg midReg3 = new VReg(BasicType.I32);
                        VReg midReg4 = new VReg(BasicType.I32);
                        mFunc.getIrs().add(new LoadItemMIR(innerOffset.getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg1, innerOffset.getRight()));
                        mFunc.getIrs().add(new LiMIR(midReg2, getElementPtrInst.getType().baseType().getSize() / 8));
                        switch (getElementPtrInst.getLastOperand()) {
                            case Argument indexArg ->
                                    mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg3, argOffsets.get(indexArg).getRight()));
                            case Instruction indexInst ->
                                    mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg3, instRegMap.get(indexInst)));
                            case ConstantNumber value -> {
                                if (value.getType() == BasicType.FLOAT) {
                                    VReg midReg = new VReg(BasicType.I32);
                                    mFunc.getIrs().add(new LiMIR(midReg, Float.floatToIntBits(value.floatValue())));
                                    mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg3, midReg));
                                } else
                                    mFunc.getIrs().add(new LiMIR(midReg3, value.intValue()));
                            }
                            default ->
                                    throw new IllegalStateException("Unexpected value: " + getElementPtrInst.getLastOperand());
                        }
                        mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.MUL, midReg4, midReg2, midReg3));
                        mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.ADD, instRegMap.get(getElementPtrInst), midReg1, midReg4));
                    }
                    if (pointer instanceof AllocaInst allocaInst) {
                        if (getElementPtrInst.size() == 3) {
                            VReg midReg1 = new VReg(BasicType.I32);
                            VReg midReg2 = new VReg(BasicType.I32);
                            VReg midReg3 = new VReg(BasicType.I32);
                            VReg midReg4 = new VReg(BasicType.I32);
                            mFunc.getIrs().add(new AddRegLocalMIR(midReg1, localOffsets.get(allocaInst)));
                            switch (getElementPtrInst.getLastOperand()) {
                                case Argument arg ->
                                        mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg2, argOffsets.get(arg).getRight()));
                                case Instruction indexInst ->
                                        mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg2, instRegMap.get(indexInst)));
                                case ConstantNumber value -> {
                                    if (value.getType() == BasicType.FLOAT) {
                                        VReg midReg = new VReg(BasicType.I32);
                                        mFunc.getIrs().add(new LiMIR(midReg, Float.floatToIntBits(value.floatValue())));
                                        mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg2, midReg));
                                    } else
                                        mFunc.getIrs().add(new LiMIR(midReg2, value.intValue()));
                                }
                                default ->
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
                            switch (getElementPtrInst.getLastOperand()) {
                                case Argument arg ->
                                        mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg2, argOffsets.get(arg).getRight()));
                                case Instruction indexInst ->
                                        mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg2, instRegMap.get(indexInst)));
                                case ConstantNumber value -> {
                                    if (value.getType() == BasicType.FLOAT) {
                                        VReg midReg = new VReg(BasicType.I32);
                                        mFunc.getIrs().add(new LiMIR(midReg, Float.floatToIntBits(value.floatValue())));
                                        mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg2, midReg));
                                    } else
                                        mFunc.getIrs().add(new LiMIR(midReg2, value.intValue()));
                                }
                                default ->
                                        throw new IllegalStateException("Unexpected value: " + getElementPtrInst.getLastOperand());
                            }
                            mFunc.getIrs().add(new LiMIR(midReg3, pointer.getType().baseType().getSize() / 8));
                            mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.MUL, midReg4, midReg2, midReg3));
                            mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.ADD, instRegMap.get(getElementPtrInst), midReg1, midReg4));
                        }
                        continue;
                    }
                    if (pointer instanceof Instruction pointerInst) {
                        if (getElementPtrInst.size() == 3) {
                            VReg midReg1 = new VReg(BasicType.I32);
                            VReg midReg2 = new VReg(BasicType.I32);
                            VReg midReg3 = new VReg(BasicType.I32);
                            switch (getElementPtrInst.getLastOperand()) {
                                case Argument arg ->
                                        mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg1, argOffsets.get(arg).getRight()));
                                case Instruction indexInst ->
                                        mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg1, instRegMap.get(indexInst)));
                                case ConstantNumber value -> {
                                    if (value.getType() == BasicType.FLOAT) {
                                        VReg midReg = new VReg(BasicType.I32);
                                        mFunc.getIrs().add(new LiMIR(midReg, Float.floatToIntBits(value.floatValue())));
                                        mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg1, midReg));
                                    } else
                                        mFunc.getIrs().add(new LiMIR(midReg1, value.intValue()));
                                }
                                default ->
                                        throw new IllegalStateException("Unexpected value: " + getElementPtrInst.getLastOperand());
                            }
                            mFunc.getIrs().add(new LiMIR(midReg2, pointer.getType().baseType().baseType().getSize() / 8));
                            mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.MUL, midReg3, midReg1, midReg2));
                            mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.ADD, instRegMap.get(getElementPtrInst), instRegMap.get(pointerInst), midReg3));
                        } else {
                            VReg midReg1 = new VReg(BasicType.I32);
                            VReg midReg2 = new VReg(BasicType.I32);
                            VReg midReg3 = new VReg(BasicType.I32);
                            switch (getElementPtrInst.getLastOperand()) {
                                case Argument arg ->
                                        mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg1, argOffsets.get(arg).getRight()));
                                case Instruction indexInst ->
                                        mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg1, instRegMap.get(indexInst)));
                                case ConstantNumber value -> {
                                    if (value.getType() == BasicType.FLOAT) {
                                        VReg midReg = new VReg(BasicType.I32);
                                        mFunc.getIrs().add(new LiMIR(midReg, Float.floatToIntBits(value.floatValue())));
                                        mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg1, midReg));
                                    } else
                                        mFunc.getIrs().add(new LiMIR(midReg1, value.intValue()));
                                }
                                default ->
                                        throw new IllegalStateException("Unexpected value: " + getElementPtrInst.getLastOperand());
                            }
                            mFunc.getIrs().add(new LiMIR(midReg2, pointer.getType().baseType().getSize() / 8));
                            mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.MUL, midReg3, midReg1, midReg2));
                            mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.ADD, instRegMap.get(getElementPtrInst), instRegMap.get(pointerInst), midReg3));
                        }
                    }
                    continue;
                }
                if (inst instanceof LoadInst loadInst) {
                    Value pointer = loadInst.getOperand(0);
                    if (pointer instanceof GlobalVariable global) {
                        VReg midReg = new VReg(BasicType.I32);
                        mFunc.getIrs().add(new LlaMIR(midReg, global));
                        mFunc.getIrs().add(new LoadMIR(instRegMap.get(loadInst), midReg, 0, 4));
                        continue;
                    }
                    if (pointer instanceof Argument arg) {
                        Pair<Boolean, Integer> innerOffset = argOffsets.get(arg);
                        mFunc.getIrs().add(new LoadItemMIR(innerOffset.getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, instRegMap.get(loadInst), innerOffset.getRight()));
                    }
                    if (pointer instanceof AllocaInst allocaInst) {
                        VReg midReg = new VReg(BasicType.I32);
                        mFunc.getIrs().add(new AddRegLocalMIR(midReg, localOffsets.get(allocaInst)));
                        mFunc.getIrs().add(new LoadMIR(instRegMap.get(loadInst), midReg, 0, allocaInst.getType().baseType().getSize() / 8));
                        continue;
                    }
                    if (pointer instanceof Instruction pointerInst) {
                        mFunc.getIrs().add(new LoadMIR(instRegMap.get(loadInst), instRegMap.get(pointerInst), 0, pointerInst.getType().baseType().getSize() / 8));
                    }
                    continue;
                }
                if (inst instanceof RetInst retInst) {
                    if (!retInst.isEmpty()) {
                        Value retVal = retInst.getOperand(0);
                        switch (retVal) {
                            case Argument arg ->
                                    mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, retVal.getType() == BasicType.I32 ? MReg.A0 : MReg.FA0, argOffsets.get(arg).getRight()));
                            case Instruction valueInst ->
                                    mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, retVal.getType() == BasicType.I32 ? MReg.A0 : MReg.FA0, instRegMap.get(valueInst)));
                            case ConstantNumber value -> {
                                switch (value.getType()) {
                                    case BasicType.I32 -> mFunc.getIrs().add(new LiMIR(MReg.A0, value.intValue()));
                                    case BasicType.FLOAT -> {
                                        VReg midReg = new VReg(BasicType.I32);
                                        mFunc.getIrs().add(new LiMIR(midReg, Float.floatToIntBits(value.floatValue())));
                                        mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, MReg.FA0, midReg));
                                    }
                                    default -> throw new IllegalStateException("Unexpected value: " + value.getType());
                                }
                            }
                            default -> throw new IllegalStateException("Unexpected value: " + retVal);
                        }
                    }
                    mFunc.getIrs().add(new BMIR(null, null, null, retLabelMIR.getBlock()));
                    continue;
                }
                if (inst instanceof StoreInst storeInst) {
                    Value value = storeInst.getOperand(0);
                    Value pointer = storeInst.getOperand(1);
                    if (pointer instanceof GlobalVariable global) {
                        VReg midReg1 = new VReg(BasicType.I32);
                        VReg midReg2 = new VReg(BasicType.I32);
                        mFunc.getIrs().add(new LlaMIR(midReg1, global));
                        switch (value) {
                            case Argument arg ->
                                    mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg2, argOffsets.get(arg).getRight()));
                            case Instruction valueInst ->
                                    mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg2, instRegMap.get(valueInst)));
                            case ConstantNumber v -> {
                                if (v.getType() == BasicType.FLOAT) {
                                    VReg midReg = new VReg(BasicType.I32);
                                    mFunc.getIrs().add(new LiMIR(midReg, Float.floatToIntBits(v.floatValue())));
                                    mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg2, midReg));
                                } else
                                    mFunc.getIrs().add(new LiMIR(midReg2, v.intValue()));
                            }
                            default -> throw new IllegalStateException("Unexpected value: " + value);
                        }
                        mFunc.getIrs().add(new StoreMIR(midReg2, midReg1, 0, 4));
                        continue;
                    }
                    if (pointer instanceof Argument arg) {
                        Pair<Boolean, Integer> innerOffset = argOffsets.get(arg);
                        VReg midReg1 = new VReg(BasicType.I32);
                        VReg midReg2 = new VReg(BasicType.I32);
                        mFunc.getIrs().add(new LoadItemMIR(innerOffset.getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg1, innerOffset.getRight()));
                        switch (value) {
                            case Argument valueArg ->
                                    mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg2, argOffsets.get(valueArg).getRight()));
                            case Instruction valueInst ->
                                    mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg2, instRegMap.get(valueInst)));
                            case ConstantNumber v -> {
                                if (v.getType() == BasicType.FLOAT) {
                                    VReg midReg = new VReg(BasicType.I32);
                                    mFunc.getIrs().add(new LiMIR(midReg, Float.floatToIntBits(v.floatValue())));
                                    mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg2, midReg));
                                } else
                                    mFunc.getIrs().add(new LiMIR(midReg2, v.intValue()));
                            }
                            default -> throw new IllegalStateException("Unexpected value: " + value);
                        }
                        mFunc.getIrs().add(new StoreMIR(midReg2, midReg1, 0, 4));
                    }
                    if (pointer instanceof AllocaInst allocaInst) {
                        VReg midReg1 = new VReg(BasicType.I32);
                        VReg midReg2 = new VReg(BasicType.I32);
                        switch (value) {
                            case Argument arg ->
                                    mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg1, argOffsets.get(arg).getRight()));
                            case Instruction valueInst ->
                                    mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg1, instRegMap.get(valueInst)));
                            case ConstantNumber v -> {
                                if (v.getType() == BasicType.FLOAT) {
                                    VReg midReg = new VReg(BasicType.I32);
                                    mFunc.getIrs().add(new LiMIR(midReg, Float.floatToIntBits(v.floatValue())));
                                    mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg1, midReg));
                                } else
                                    mFunc.getIrs().add(new LiMIR(midReg1, v.intValue()));
                            }
                            default -> throw new IllegalStateException("Unexpected value: " + value);
                        }
                        mFunc.getIrs().add(new AddRegLocalMIR(midReg2, localOffsets.get(allocaInst)));
                        mFunc.getIrs().add(new StoreMIR(midReg1, midReg2, 0, allocaInst.getType().baseType().getSize() / 8));
                        continue;
                    }
                    if (pointer instanceof Instruction pointerInst) {
                        VReg midReg = new VReg(BasicType.I32);
                        switch (value) {
                            case Argument arg ->
                                    mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg, argOffsets.get(arg).getRight()));
                            case Instruction valueInst ->
                                    mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg, instRegMap.get(valueInst)));
                            case ConstantNumber v -> {
                                if (v.getType() == BasicType.FLOAT) {
                                    VReg midReg1 = new VReg(BasicType.I32);
                                    mFunc.getIrs().add(new LiMIR(midReg1, Float.floatToIntBits(v.floatValue())));
                                    mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg, midReg1));
                                } else
                                    mFunc.getIrs().add(new LiMIR(midReg, v.intValue()));
                            }
                            default -> throw new IllegalStateException("Unexpected value: " + value);
                        }
                        mFunc.getIrs().add(new StoreMIR(midReg, instRegMap.get(pointerInst), 0, pointerInst.getType().baseType().getSize() / 8));
                    }
                    continue;
                }
                if (inst instanceof AllocaInst) {
                    continue;
                }
                if (inst instanceof BitCastInst bitCastInst) {
                    Instruction operand = bitCastInst.getOperand(0);
                    VReg srcReg = instRegMap.get(operand);
                    if (operand instanceof AllocaInst allocaInst) {
                        srcReg = new VReg(BasicType.I32);
                        mFunc.getIrs().add(new AddRegLocalMIR(srcReg, localOffsets.get(allocaInst)));
                    }
                    mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, instRegMap.get(bitCastInst), srcReg));
                    continue;
                }
                if (inst instanceof ICmpInst iCmpInst) {
                    VReg target = instRegMap.get(iCmpInst);
                    VReg operand1 = switch (iCmpInst.getOperand(0)) {
                        case Argument arg -> {
                            VReg midReg = new VReg(arg.getType() == BasicType.FLOAT ? BasicType.FLOAT : BasicType.I32);
                            mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg, argOffsets.get(arg).getRight()));
                            yield midReg;
                        }
                        case Instruction valueInst -> instRegMap.get(valueInst);
                        case ConstantNumber value -> {
                            VReg midReg = new VReg(value.getType());
                            mFunc.getIrs().add(new LiMIR(midReg, value.intValue()));
                            yield midReg;
                        }
                        default -> throw new IllegalStateException("Unexpected value: " + iCmpInst.getOperand(0));
                    };
                    VReg operand2 = switch (iCmpInst.getOperand(1)) {
                        case Argument arg -> {
                            VReg midReg = new VReg(arg.getType() == BasicType.FLOAT ? BasicType.FLOAT : BasicType.I32);
                            mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg, argOffsets.get(arg).getRight()));
                            yield midReg;
                        }
                        case Instruction valueInst -> instRegMap.get(valueInst);
                        case ConstantNumber value -> {
                            VReg midReg = new VReg(value.getType());
                            mFunc.getIrs().add(new LiMIR(midReg, value.intValue()));
                            yield midReg;
                        }
                        default -> throw new IllegalStateException("Unexpected value: " + iCmpInst.getOperand(1));
                    };
                    switch (iCmpInst.getCond()) {
                        case EQ -> {
                            VReg midReg = new VReg(BasicType.I32);
                            mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.SUB, midReg, operand1, operand2));
                            mFunc.getIrs().add(new RrMIR(RrMIR.Op.SEQZ, target, midReg));
                        }
                        case NE -> {
                            VReg midReg = new VReg(BasicType.I32);
                            mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.SUB, midReg, operand1, operand2));
                            mFunc.getIrs().add(new RrMIR(RrMIR.Op.SNEZ, target, midReg));
                        }
                        case SGE -> {
                            VReg midReg = new VReg(BasicType.I32);
                            mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.SLT, midReg, operand1, operand2));
                            mFunc.getIrs().add(new RriMIR(RriMIR.Op.XORI, target, midReg, 1));
                        }
                        case SGT -> mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.SGT, target, operand1, operand2));
                        case SLE -> {
                            VReg midReg = new VReg(BasicType.I32);
                            mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.SGT, midReg, operand1, operand2));
                            mFunc.getIrs().add(new RriMIR(RriMIR.Op.XORI, target, midReg, 1));
                        }
                        case SLT -> mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.SLT, target, operand1, operand2));
                    }
                    continue;
                }
                if (inst instanceof FCmpInst fCmpInst) {
                    VReg target = instRegMap.get(fCmpInst);
                    VReg operand1 = switch (fCmpInst.getOperand(0)) {
                        case Argument arg -> {
                            VReg midReg = new VReg(arg.getType() == BasicType.FLOAT ? BasicType.FLOAT : BasicType.I32);
                            mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg, argOffsets.get(arg).getRight()));
                            yield midReg;
                        }
                        case Instruction valueInst -> instRegMap.get(valueInst);
                        case ConstantNumber value -> {
                            VReg midReg1 = new VReg(BasicType.I32);
                            VReg midReg2 = new VReg(BasicType.FLOAT);
                            mFunc.getIrs().add(new LiMIR(midReg1, Float.floatToIntBits(value.floatValue())));
                            mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg2, midReg1));
                            yield midReg2;
                        }
                        default -> throw new IllegalStateException("Unexpected value: " + fCmpInst.getOperand(0));
                    };
                    VReg operand2 = switch (fCmpInst.getOperand(1)) {
                        case Argument arg -> {
                            VReg midReg = new VReg(arg.getType() == BasicType.FLOAT ? BasicType.FLOAT : BasicType.I32);
                            mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg, argOffsets.get(arg).getRight()));
                            yield midReg;
                        }
                        case Instruction valueInst -> instRegMap.get(valueInst);
                        case ConstantNumber value -> {
                            VReg midReg1 = new VReg(BasicType.I32);
                            VReg midReg2 = new VReg(BasicType.FLOAT);
                            mFunc.getIrs().add(new LiMIR(midReg1, Float.floatToIntBits(value.floatValue())));
                            mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg2, midReg1));
                            yield midReg2;
                        }
                        default -> throw new IllegalStateException("Unexpected value: " + fCmpInst.getOperand(1));
                    };
                    if (fCmpInst.getCond() == FCmpInst.Cond.UNE) {
                        mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.EQ, target, operand1, operand2));
                        mFunc.getIrs().add(new RriMIR(RriMIR.Op.XORI, target, target, 1));
                        continue;
                    }
                    mFunc.getIrs().add(new RrrMIR(switch (fCmpInst.getCond()) {
                        case OEQ -> RrrMIR.Op.EQ;
                        case OGE -> RrrMIR.Op.GE;
                        case OGT -> RrrMIR.Op.GT;
                        case OLE -> RrrMIR.Op.LE;
                        case OLT -> RrrMIR.Op.LT;
                        default -> throw new IllegalStateException("Unexpected value: " + fCmpInst.getCond());
                    }, target, operand1, operand2));
                    continue;
                }
                if (inst instanceof ZExtInst zExtInst) {
                    VReg operand = switch (zExtInst.getOperand(0)) {
                        case Argument arg -> {
                            VReg midReg = new VReg(arg.getType() == BasicType.FLOAT ? BasicType.FLOAT : BasicType.I32);
                            mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg, argOffsets.get(arg).getRight()));
                            yield midReg;
                        }
                        case Instruction valueInst -> instRegMap.get(valueInst);
                        case ConstantNumber value -> {
                            VReg midReg = new VReg(value.getType());
                            mFunc.getIrs().add(new LiMIR(midReg, value.intValue()));
                            yield midReg;
                        }
                        default -> throw new IllegalStateException("Unexpected value: " + zExtInst.getOperand(0));
                    };
                    mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, instRegMap.get(zExtInst), operand));
                    continue;
                }
                if (inst instanceof SExtInst sExtInst) {
                    VReg operand = switch (sExtInst.getOperand(0)) {
                        case Argument arg -> {
                            VReg midReg = new VReg(arg.getType() == BasicType.FLOAT ? BasicType.FLOAT : BasicType.I32);
                            mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg, argOffsets.get(arg).getRight()));
                            yield midReg;
                        }
                        case Instruction valueInst -> instRegMap.get(valueInst);
                        case ConstantNumber value -> {
                            VReg midReg = new VReg(value.getType());
                            mFunc.getIrs().add(new LiMIR(midReg, value.intValue()));
                            yield midReg;
                        }
                        default -> throw new IllegalStateException("Unexpected value: " + sExtInst.getOperand(0));
                    };
                    mFunc.getIrs().add(new RrMIR(RrMIR.Op.NEG, instRegMap.get(sExtInst), operand));
                    continue;
                }
                if (inst instanceof FPToSIInst fpToSIInst) {
                    VReg operand = switch (fpToSIInst.getOperand(0)) {
                        case Argument arg -> {
                            VReg midReg = new VReg(arg.getType() == BasicType.FLOAT ? BasicType.FLOAT : BasicType.I32);
                            mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg, argOffsets.get(arg).getRight()));
                            yield midReg;
                        }
                        case Instruction valueInst -> instRegMap.get(valueInst);
                        case ConstantNumber value -> {
                            VReg midReg1 = new VReg(BasicType.I32);
                            VReg midReg2 = new VReg(BasicType.FLOAT);
                            mFunc.getIrs().add(new LiMIR(midReg1, Float.floatToIntBits(value.floatValue())));
                            mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg2, midReg1));
                            yield midReg2;
                        }
                        default -> throw new IllegalStateException("Unexpected value: " + fpToSIInst.getOperand(0));
                    };
                    mFunc.getIrs().add(new RrMIR(RrMIR.Op.CVT, instRegMap.get(fpToSIInst), operand));
                    continue;
                }
                if (inst instanceof SIToFPInst siToFPInst) {
                    VReg operand = switch (siToFPInst.getOperand(0)) {
                        case Argument arg -> {
                            VReg midReg = new VReg(arg.getType() == BasicType.FLOAT ? BasicType.FLOAT : BasicType.I32);
                            mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg, argOffsets.get(arg).getRight()));
                            yield midReg;
                        }
                        case Instruction valueInst -> instRegMap.get(valueInst);
                        case ConstantNumber value -> {
                            VReg midReg = new VReg(value.getType());
                            mFunc.getIrs().add(new LiMIR(midReg, value.intValue()));
                            yield midReg;
                        }
                        default -> throw new IllegalStateException("Unexpected value: " + siToFPInst.getOperand(0));
                    };
                    mFunc.getIrs().add(new RrMIR(RrMIR.Op.CVT, instRegMap.get(siToFPInst), operand));
                    continue;
                }
                if (inst instanceof FakeMvInst fakeMvInst) {
                    PHINode phiNode = fakeMvInst.getOperand(0);
                    VReg target = instRegMap.get(phiNode);
                    VReg source = switch (fakeMvInst.getOperand(1)) {
                        case Argument arg -> {
                            VReg midReg = new VReg(arg.getType() == BasicType.FLOAT ? BasicType.FLOAT : BasicType.I32);
                            mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg, argOffsets.get(arg).getRight()));
                            yield midReg;
                        }
                        case Instruction inst1 -> instRegMap.get(inst1);
                        case ConstantNumber value -> {
                            VReg midReg = new VReg(value.getType());
                            switch (midReg.getType()) {
                                case BasicType.I32 -> mFunc.getIrs().add(new LiMIR(midReg, value.intValue()));
                                case BasicType.FLOAT -> {
                                    VReg midReg1 = new VReg(BasicType.I32);
                                    mFunc.getIrs().add(new LiMIR(midReg1, Float.floatToIntBits(value.floatValue())));
                                    mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg, midReg1));
                                }
                                default -> throw new IllegalStateException("Unexpected value: " + midReg.getType());
                            }
                            yield midReg;
                        }
                        default -> null;
                    };
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
