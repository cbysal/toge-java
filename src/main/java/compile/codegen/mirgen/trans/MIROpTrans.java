package compile.codegen.mirgen.trans;

import compile.codegen.MReg;
import compile.codegen.VReg;
import compile.codegen.mirgen.mir.*;
import compile.llvm.Argument;
import compile.llvm.BasicBlock;
import compile.llvm.Function;
import compile.llvm.GlobalVariable;
import compile.llvm.contant.ConstantNumber;
import compile.llvm.ir.*;
import compile.llvm.type.BasicType;
import compile.llvm.type.PointerType;
import compile.llvm.value.Value;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class MIROpTrans {
    public static void transBranch(List<MIR> irs, Map<Instruction, VReg> instRegMap, BranchInst branchInst) {
        if (!branchInst.isConditional()) {
            BasicBlock dest = branchInst.getOperand(0);
            irs.add(new BMIR(null, null, null, dest));
            return;
        }
        Value cond = branchInst.getOperand(0);
        BasicBlock ifTrue = branchInst.getOperand(1);
        BasicBlock ifFalse = branchInst.getOperand(2);
        VReg reg;
        if (Objects.requireNonNull(cond) instanceof Instruction) {
            reg = instRegMap.get(cond);
        } else if (cond instanceof ConstantNumber) {
            ConstantNumber value = (ConstantNumber) cond;
            VReg midReg = new VReg(value.getType());
            if (value.getType() == BasicType.FLOAT)
                MIROpHelper.loadImmToReg(irs, midReg, value.floatValue());
            else
                MIROpHelper.loadImmToReg(irs, midReg, value.intValue());
            reg = midReg;
        } else {
            throw new IllegalStateException("Unexpected value: " + cond);
        }
        irs.add(new BMIR(BMIR.Op.NE, reg, MReg.ZERO, ifTrue));
        irs.add(new BMIR(null, null, null, ifFalse));
    }

    public static void transBinary(List<MIR> irs, Map<Instruction, VReg> instRegMap, Map<Argument, Pair<Boolean, Integer>> argOffsets, BinaryOperator binaryOperator) {
        Value operand1 = binaryOperator.getOperand(0);
        Value operand2 = binaryOperator.getOperand(1);
        VReg reg1;
        if (Objects.requireNonNull(operand1) instanceof Argument) {
            Argument arg = (Argument) operand1;
            VReg midReg = new VReg(arg.getType() == BasicType.FLOAT ? BasicType.FLOAT : BasicType.I32);
            irs.add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg, argOffsets.get(arg).getRight()));
            reg1 = midReg;
        } else if (operand1 instanceof Instruction) {
            Instruction inst = (Instruction) operand1;
            reg1 = instRegMap.get(inst);
        } else {
            reg1 = null;
        }
        VReg reg2;
        if (Objects.requireNonNull(operand2) instanceof Argument) {
            Argument arg = (Argument) operand2;
            VReg midReg = new VReg(arg.getType() == BasicType.FLOAT ? BasicType.FLOAT : BasicType.I32);
            irs.add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg, argOffsets.get(arg).getRight()));
            reg2 = midReg;
        } else if (operand2 instanceof Instruction) {
            Instruction inst = (Instruction) operand2;
            reg2 = instRegMap.get(inst);
        } else {
            reg2 = null;
        }
        if (reg1 != null && reg2 != null) {
            MIRBinaryTrans.transBinaryRegReg(irs, instRegMap, binaryOperator, reg1, reg2);
            return;
        }
        if (reg1 != null && operand2 instanceof ConstantNumber) {
            ConstantNumber value2 = (ConstantNumber) operand2;
            MIRBinaryTrans.transBinaryRegImm(irs, instRegMap, binaryOperator, reg1, value2);
            return;
        }
        if (operand1 instanceof ConstantNumber && reg2 != null) {
            ConstantNumber value1 = (ConstantNumber) operand1;
            MIRBinaryTrans.transBinaryImmReg(irs, instRegMap, binaryOperator, value1, reg2);
            return;
        }
        if (operand1 instanceof ConstantNumber && operand2 instanceof ConstantNumber) {
            ConstantNumber value1 = (ConstantNumber) operand1;
            ConstantNumber value2 = (ConstantNumber) operand2;
            VReg midReg = new VReg(value1.getType());
            if (value1.getType().equals(BasicType.I32)) {
                MIROpHelper.loadImmToReg(irs, midReg, value1.intValue());
            } else if (value1.getType().equals(BasicType.FLOAT)) {
                MIROpHelper.loadImmToReg(irs, midReg, value1.floatValue());
            } else {
                throw new IllegalStateException("Unexpected value: " + value1.getType());
            }
            MIRBinaryTrans.transBinaryRegImm(irs, instRegMap, binaryOperator, midReg, value2);
            return;
        }
        throw new RuntimeException();
    }

    public static int transCall(List<MIR> irs, Map<Instruction, VReg> instRegMap, Map<Argument, Pair<Boolean, Integer>> argOffsets, CallInst callInst, Map<AllocaInst, Integer> localOffsets) {
        Function func = callInst.getOperand(0);
        List<MIR> saveCalleeIRs = new ArrayList<>();
        int iSize = 0, fSize = 0;
        for (int i = 1; i < callInst.size(); i++) {
            Value param = callInst.getOperand(i);
            if (param.getType() == BasicType.FLOAT) {
                if (fSize < MReg.F_CALLER_REGS.size()) {
                    if (param instanceof Argument) {
                        Argument arg = (Argument) param;
                        saveCalleeIRs.add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, MReg.F_CALLER_REGS.get(fSize), argOffsets.get(arg).getRight()));
                    } else if (param instanceof AllocaInst) {
                        AllocaInst allocaInst = (AllocaInst) param;
                        saveCalleeIRs.add(new RrMIR(RrMIR.Op.MV, MReg.F_CALLER_REGS.get(fSize), instRegMap.get(allocaInst)));
                    } else if (param instanceof Instruction) {
                        Instruction inst = (Instruction) param;
                        saveCalleeIRs.add(new RrMIR(RrMIR.Op.MV, MReg.F_CALLER_REGS.get(fSize), instRegMap.get(inst)));
                    } else if (param instanceof ConstantNumber) {
                        ConstantNumber value = (ConstantNumber) param;
                        MIROpHelper.loadImmToReg(saveCalleeIRs, MReg.F_CALLER_REGS.get(fSize), value.floatValue());
                    } else {
                        throw new IllegalStateException("Unexpected value: " + param);
                    }
                } else {
                    if (param instanceof Argument) {
                        Argument arg = (Argument) param;
                        VReg midReg = new VReg(arg.getType());
                        irs.add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg, argOffsets.get(arg).getRight()));
                        irs.add(new StoreItemMIR(StoreItemMIR.Item.PARAM_CALL, midReg, (Integer.max(iSize - MReg.I_CALLER_REGS.size(), 0) + Integer.max(fSize - MReg.I_CALLER_REGS.size(), 0)) * 8));
                    } else if (param instanceof Instruction) {
                        Instruction inst = (Instruction) param;
                        irs.add(new StoreItemMIR(StoreItemMIR.Item.PARAM_CALL, instRegMap.get(inst), (Integer.max(iSize - MReg.I_CALLER_REGS.size(), 0) + Integer.max(fSize - MReg.I_CALLER_REGS.size(), 0)) * 8));
                    } else if (param instanceof ConstantNumber) {
                        ConstantNumber value = (ConstantNumber) param;
                        VReg midReg = new VReg(BasicType.I32);
                        MIROpHelper.loadImmToReg(irs, midReg, value.floatValue());
                        irs.add(new StoreItemMIR(StoreItemMIR.Item.PARAM_CALL, midReg, (Integer.max(iSize - MReg.I_CALLER_REGS.size(), 0) + Integer.max(fSize - MReg.I_CALLER_REGS.size(), 0)) * 8));
                    } else {
                        throw new IllegalStateException("Unexpected value: " + param);
                    }
                }
                fSize++;
            } else {
                if (iSize < MReg.I_CALLER_REGS.size()) {
                    if (param instanceof GlobalVariable) {
                        GlobalVariable global = (GlobalVariable) param;
                        saveCalleeIRs.add(new LlaMIR(MReg.I_CALLER_REGS.get(iSize), global));
                    } else if (param instanceof AllocaInst) {
                        AllocaInst allocaInst = (AllocaInst) param;
                        if (allocaInst.getType() instanceof PointerType)
                            saveCalleeIRs.add(new AddRegLocalMIR(MReg.I_CALLER_REGS.get(iSize), localOffsets.get(allocaInst)));
                        else
                            saveCalleeIRs.add(new RrMIR(RrMIR.Op.MV, MReg.I_CALLER_REGS.get(iSize), instRegMap.get(allocaInst)));
                    } else if (param instanceof Argument) {
                        Argument arg = (Argument) param;
                        saveCalleeIRs.add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, MReg.I_CALLER_REGS.get(iSize), argOffsets.get(arg).getRight()));
                    } else if (param instanceof Instruction) {
                        Instruction inst = (Instruction) param;
                        saveCalleeIRs.add(new RrMIR(RrMIR.Op.MV, MReg.I_CALLER_REGS.get(iSize), instRegMap.get(inst)));
                    } else if (param instanceof ConstantNumber) {
                        ConstantNumber value = (ConstantNumber) param;
                        MIROpHelper.loadImmToReg(saveCalleeIRs, MReg.I_CALLER_REGS.get(iSize), value.intValue());
                    } else {
                        throw new IllegalStateException("Unexpected value: " + param);
                    }
                } else {
                    if (param instanceof AllocaInst) {
                        AllocaInst allocaInst = (AllocaInst) param;
                        VReg midReg = new VReg(BasicType.I32);
                        irs.add(new AddRegLocalMIR(midReg, localOffsets.get(allocaInst)));
                        irs.add(new StoreItemMIR(StoreItemMIR.Item.PARAM_CALL, midReg, (Integer.max(iSize - MReg.I_CALLER_REGS.size(), 0) + Integer.max(fSize - MReg.I_CALLER_REGS.size(), 0)) * 8));
                    } else if (param instanceof Argument) {
                        Argument arg = (Argument) param;
                        VReg midReg = new VReg(arg.getType());
                        irs.add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg, argOffsets.get(arg).getRight()));
                        irs.add(new StoreItemMIR(StoreItemMIR.Item.PARAM_CALL, midReg, (Integer.max(iSize - MReg.I_CALLER_REGS.size(), 0) + Integer.max(fSize - MReg.I_CALLER_REGS.size(), 0)) * 8));
                    } else if (param instanceof Instruction) {
                        Instruction inst = (Instruction) param;
                        irs.add(new StoreItemMIR(StoreItemMIR.Item.PARAM_CALL, instRegMap.get(inst), (Integer.max(iSize - MReg.I_CALLER_REGS.size(), 0) + Integer.max(fSize - MReg.I_CALLER_REGS.size(), 0)) * 8));
                    } else if (param instanceof ConstantNumber) {
                        ConstantNumber value = (ConstantNumber) param;
                        VReg midReg = new VReg(BasicType.I32);
                        MIROpHelper.loadImmToReg(irs, midReg, value.intValue());
                        irs.add(new StoreItemMIR(StoreItemMIR.Item.PARAM_CALL, midReg, (Integer.max(iSize - MReg.I_CALLER_REGS.size(), 0) + Integer.max(fSize - MReg.I_CALLER_REGS.size(), 0)) * 8));
                    } else {
                        throw new IllegalStateException("Unexpected value: " + param);
                    }
                }
                iSize++;
            }
        }
        irs.addAll(saveCalleeIRs);
        irs.add(new CallMIR(func));
        if (callInst.getType() != BasicType.VOID) {
            VReg target = instRegMap.get(callInst);
            MReg reg;
            if (callInst.getType().equals(BasicType.I32)) {
                reg = MReg.A0;
            } else if (callInst.getType().equals(BasicType.FLOAT)) {
                reg = MReg.FA0;
            } else {
                throw new IllegalStateException("Unexpected value: " + callInst.getType());
            }
            irs.add(new RrMIR(RrMIR.Op.MV, target, reg));
        }
        return callInst.size() - 1;
    }
}
