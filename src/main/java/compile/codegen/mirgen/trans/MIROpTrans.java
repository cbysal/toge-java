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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        VReg reg = switch (cond) {
            case Instruction ir -> instRegMap.get(ir);
            case ConstantNumber value -> {
                VReg midReg = new VReg(value.getType());
                if (value.getType() == BasicType.FLOAT)
                    MIROpHelper.loadImmToReg(irs, midReg, value.floatValue());
                else
                    MIROpHelper.loadImmToReg(irs, midReg, value.intValue());
                yield midReg;
            }
            default -> throw new IllegalStateException("Unexpected value: " + cond);
        };
        irs.add(new BMIR(BMIR.Op.NE, reg, MReg.ZERO, ifTrue));
        irs.add(new BMIR(null, null, null, ifFalse));
    }

    public static void transBinary(List<MIR> irs, Map<Instruction, VReg> instRegMap, Map<Argument, VReg> argRegMap, BinaryOperator binaryOperator) {
        Value operand1 = binaryOperator.getOperand(0);
        Value operand2 = binaryOperator.getOperand(1);
        VReg reg1 = switch (operand1) {
            case Argument arg -> argRegMap.get(arg);
            case Instruction inst -> instRegMap.get(inst);
            default -> null;
        };
        VReg reg2 = switch (operand2) {
            case Argument arg -> argRegMap.get(arg);
            case Instruction inst -> instRegMap.get(inst);
            default -> null;
        };
        if (reg1 != null && reg2 != null) {
            MIRBinaryTrans.transBinaryRegReg(irs, instRegMap, binaryOperator, reg1, reg2);
            return;
        }
        if (reg1 != null && operand2 instanceof ConstantNumber value2) {
            MIRBinaryTrans.transBinaryRegImm(irs, instRegMap, binaryOperator, reg1, value2);
            return;
        }
        if (operand1 instanceof ConstantNumber value1 && reg2 != null) {
            MIRBinaryTrans.transBinaryImmReg(irs, instRegMap, binaryOperator, value1, reg2);
            return;
        }
        if (operand1 instanceof ConstantNumber value1 && operand2 instanceof ConstantNumber value2) {
            VReg midReg = new VReg(value1.getType());
            switch (value1.getType()) {
                case BasicType.I32 -> MIROpHelper.loadImmToReg(irs, midReg, value1.intValue());
                case BasicType.FLOAT -> MIROpHelper.loadImmToReg(irs, midReg, value1.floatValue());
                default -> throw new IllegalStateException("Unexpected value: " + value1.getType());
            }
            MIRBinaryTrans.transBinaryRegImm(irs, instRegMap, binaryOperator, midReg, value2);
            return;
        }
        throw new RuntimeException();
    }

    public static int transCall(List<MIR> irs, Map<Instruction, VReg> instRegMap, Map<Argument, VReg> argRegMap, CallInst callInst, Map<AllocaInst, Integer> localOffsets) {
        Function func = callInst.getOperand(0);
        List<MIR> saveCalleeIRs = new ArrayList<>();
        int iSize = 0, fSize = 0;
        for (int i = 1; i < callInst.size(); i++) {
            Value param = callInst.getOperand(i);
            if (param.getType() == BasicType.FLOAT) {
                if (fSize < MReg.F_CALLER_REGS.size()) {
                    switch (param) {
                        case Argument arg ->
                                saveCalleeIRs.add(new RrMIR(RrMIR.Op.MV, MReg.F_CALLER_REGS.get(fSize), argRegMap.get(arg)));
                        case AllocaInst allocaInst ->
                                saveCalleeIRs.add(new RrMIR(RrMIR.Op.MV, MReg.F_CALLER_REGS.get(fSize), instRegMap.get(allocaInst)));
                        case Instruction inst ->
                                saveCalleeIRs.add(new RrMIR(RrMIR.Op.MV, MReg.F_CALLER_REGS.get(fSize), instRegMap.get(inst)));
                        case ConstantNumber value ->
                                MIROpHelper.loadImmToReg(saveCalleeIRs, MReg.F_CALLER_REGS.get(fSize), value.floatValue());
                        default -> throw new IllegalStateException("Unexpected value: " + param);
                    }
                } else {
                    switch (param) {
                        case Argument arg ->
                                irs.add(new StoreItemMIR(StoreItemMIR.Item.PARAM_CALL, argRegMap.get(arg), (Integer.max(iSize - MReg.I_CALLER_REGS.size(), 0) + Integer.max(fSize - MReg.I_CALLER_REGS.size(), 0)) * 8));
                        case Instruction inst ->
                                irs.add(new StoreItemMIR(StoreItemMIR.Item.PARAM_CALL, instRegMap.get(inst), (Integer.max(iSize - MReg.I_CALLER_REGS.size(), 0) + Integer.max(fSize - MReg.I_CALLER_REGS.size(), 0)) * 8));
                        case ConstantNumber value -> {
                            VReg midReg = new VReg(BasicType.I32);
                            MIROpHelper.loadImmToReg(irs, midReg, value.floatValue());
                            irs.add(new StoreItemMIR(StoreItemMIR.Item.PARAM_CALL, midReg, (Integer.max(iSize - MReg.I_CALLER_REGS.size(), 0) + Integer.max(fSize - MReg.I_CALLER_REGS.size(), 0)) * 8));
                        }
                        default -> throw new IllegalStateException("Unexpected value: " + param);
                    }
                }
                fSize++;
            } else {
                if (iSize < MReg.I_CALLER_REGS.size()) {
                    switch (param) {
                        case GlobalVariable global ->
                                saveCalleeIRs.add(new LlaMIR(MReg.I_CALLER_REGS.get(iSize), global));
                        case AllocaInst allocaInst -> {
                            if (allocaInst.getType() instanceof PointerType)
                                saveCalleeIRs.add(new AddRegLocalMIR(MReg.I_CALLER_REGS.get(iSize), localOffsets.get(allocaInst)));
                            else
                                saveCalleeIRs.add(new RrMIR(RrMIR.Op.MV, MReg.I_CALLER_REGS.get(iSize), instRegMap.get(allocaInst)));
                        }
                        case Argument arg ->
                                saveCalleeIRs.add(new RrMIR(RrMIR.Op.MV, MReg.I_CALLER_REGS.get(iSize), argRegMap.get(arg)));
                        case Instruction inst ->
                                saveCalleeIRs.add(new RrMIR(RrMIR.Op.MV, MReg.I_CALLER_REGS.get(iSize), instRegMap.get(inst)));
                        case ConstantNumber value ->
                                MIROpHelper.loadImmToReg(saveCalleeIRs, MReg.I_CALLER_REGS.get(iSize), value.intValue());
                        default -> throw new IllegalStateException("Unexpected value: " + param);
                    }
                } else {
                    switch (param) {
                        case AllocaInst allocaInst -> {
                            VReg midReg = new VReg(BasicType.I32);
                            irs.add(new AddRegLocalMIR(midReg, localOffsets.get(allocaInst)));
                            irs.add(new StoreItemMIR(StoreItemMIR.Item.PARAM_CALL, midReg, (Integer.max(iSize - MReg.I_CALLER_REGS.size(), 0) + Integer.max(fSize - MReg.I_CALLER_REGS.size(), 0)) * 8));
                        }
                        case Argument arg ->
                                irs.add(new StoreItemMIR(StoreItemMIR.Item.PARAM_CALL, argRegMap.get(arg), (Integer.max(iSize - MReg.I_CALLER_REGS.size(), 0) + Integer.max(fSize - MReg.I_CALLER_REGS.size(), 0)) * 8));
                        case Instruction inst ->
                                irs.add(new StoreItemMIR(StoreItemMIR.Item.PARAM_CALL, instRegMap.get(inst), (Integer.max(iSize - MReg.I_CALLER_REGS.size(), 0) + Integer.max(fSize - MReg.I_CALLER_REGS.size(), 0)) * 8));
                        case ConstantNumber value -> {
                            VReg midReg = new VReg(BasicType.I32);
                            MIROpHelper.loadImmToReg(irs, midReg, value.intValue());
                            irs.add(new StoreItemMIR(StoreItemMIR.Item.PARAM_CALL, midReg, (Integer.max(iSize - MReg.I_CALLER_REGS.size(), 0) + Integer.max(fSize - MReg.I_CALLER_REGS.size(), 0)) * 8));
                        }
                        default -> throw new IllegalStateException("Unexpected value: " + param);
                    }
                }
                iSize++;
            }
        }
        irs.addAll(saveCalleeIRs);
        irs.add(new CallMIR(func));
        if (callInst.getType() != BasicType.VOID) {
            VReg target = instRegMap.get(callInst);
            irs.add(new RrMIR(RrMIR.Op.MV, target, switch (callInst.getType()) {
                case BasicType.I32 -> MReg.A0;
                case BasicType.FLOAT -> MReg.FA0;
                default -> throw new IllegalStateException("Unexpected value: " + callInst.getType());
            }));
        }
        return callInst.size() - 1;
    }
}
