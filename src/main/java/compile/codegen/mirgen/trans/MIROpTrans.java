package compile.codegen.mirgen.trans;

import compile.codegen.MReg;
import compile.codegen.VReg;
import compile.codegen.mirgen.mir.*;
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
        if (!branchInst.conditional()) {
            irs.add(new BMIR(null, null, null, branchInst.getDest().getLabel()));
            return;
        }
        VReg reg = switch (branchInst.getCond()) {
            case Instruction ir -> instRegMap.get(ir);
            case ConstantNumber value -> {
                VReg midReg = new VReg(value.getType());
                if (value.getType() == BasicType.FLOAT)
                    MIROpHelper.loadImmToReg(irs, midReg, value.floatValue());
                else
                    MIROpHelper.loadImmToReg(irs, midReg, value.intValue());
                yield midReg;
            }
            default -> throw new IllegalStateException("Unexpected value: " + branchInst.getCond());
        };
        irs.add(new BMIR(BMIR.Op.NE, reg, MReg.ZERO, branchInst.getIfTrue().getLabel()));
        irs.add(new BMIR(null, null, null, branchInst.getIfFalse().getLabel()));
    }

    public static void transBinary(List<MIR> irs, Map<Instruction, VReg> instRegMap, BinaryOperator binaryOperator) {
        if (binaryOperator.left instanceof Instruction inst1 && binaryOperator.right instanceof Instruction inst2) {
            MIRBinaryTrans.transBinaryRegReg(irs, instRegMap, binaryOperator, instRegMap.get(inst1), instRegMap.get(inst2));
            return;
        }
        if (binaryOperator.left instanceof Instruction inst1 && binaryOperator.right instanceof ConstantNumber value2) {
            MIRBinaryTrans.transBinaryRegImm(irs, instRegMap, binaryOperator, instRegMap.get(inst1), value2);
            return;
        }
        if (binaryOperator.left instanceof ConstantNumber value1 && binaryOperator.right instanceof Instruction inst2) {
            MIRBinaryTrans.transBinaryImmReg(irs, instRegMap, binaryOperator, value1, instRegMap.get(inst2));
            return;
        }
        if (binaryOperator.left instanceof ConstantNumber value1 && binaryOperator.right instanceof ConstantNumber value2) {
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

    public static int transCall(List<MIR> irs, Map<Instruction, VReg> instRegMap, CallInst callVIR, Map<AllocaInst, Integer> localOffsets) {
        List<Value> params = callVIR.params;
        List<MIR> saveCalleeIRs = new ArrayList<>();
        int iSize = 0, fSize = 0;
        for (Value param : params) {
            if (param.getType() == BasicType.FLOAT) {
                if (fSize < MReg.F_CALLER_REGS.size()) {
                    switch (param) {
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
        irs.add(new CallMIR(callVIR.func));
        if (callVIR.getType() != BasicType.VOID) {
            VReg target = instRegMap.get(callVIR);
            irs.add(new RrMIR(RrMIR.Op.MV, target, switch (callVIR.getType()) {
                case BasicType.I32 -> MReg.A0;
                case BasicType.FLOAT -> MReg.FA0;
                default -> throw new IllegalStateException("Unexpected value: " + callVIR.getType());
            }));
        }
        return params.size();
    }
}
