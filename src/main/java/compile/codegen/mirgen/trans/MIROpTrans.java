package compile.codegen.mirgen.trans;

import compile.codegen.mirgen.MReg;
import compile.codegen.mirgen.mir.*;
import compile.vir.Block;
import compile.vir.VReg;
import compile.vir.ir.*;
import compile.vir.type.BasicType;
import compile.symbol.*;
import compile.vir.value.Value;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MIROpTrans {
    public static void transBranch(List<MIR> irs, Map<VIR, VReg> virRegMap, BranchVIR branchVIR) {
        BranchVIR.Type type = branchVIR.type;
        Value lReg = branchVIR.left;
        Value rReg = branchVIR.right;
        Block trueBlock = branchVIR.trueBlock;
        Block falseBlock = branchVIR.falseBlock;
        VReg reg1 = switch (lReg) {
            case VIR ir -> {
                if (virRegMap.containsKey(ir))
                    yield virRegMap.get(ir);
                else {
                    VReg reg = new VReg(ir.getType(), ir.getType().getSize());
                    virRegMap.put(ir, reg);
                    yield reg;
                }
            }
            case VReg reg -> reg;
            case InstantValue value -> {
                VReg reg = new VReg(value.getType(), value.getType().getSize());
                if (value.getType() == BasicType.FLOAT)
                    MIROpHelper.loadImmToReg(irs, reg, value.floatValue());
                else
                    MIROpHelper.loadImmToReg(irs, reg, value.intValue());
                yield reg;
            }
            default -> throw new IllegalStateException("Unexpected value: " + lReg);
        };
        VReg reg2 = switch (rReg) {
            case VIR ir -> {
                if (virRegMap.containsKey(ir))
                    yield virRegMap.get(ir);
                else {
                    VReg reg = new VReg(ir.getType(), ir.getType().getSize());
                    virRegMap.put(ir, reg);
                    yield reg;
                }
            }
            case VReg reg -> reg;
            case InstantValue value -> {
                VReg reg = new VReg(value.getType(), value.getType().getSize());
                if (value.getType() == BasicType.FLOAT)
                    MIROpHelper.loadImmToReg(irs, reg, value.floatValue());
                else
                    MIROpHelper.loadImmToReg(irs, reg, value.intValue());
                yield reg;
            }
            default -> throw new IllegalStateException("Unexpected value: " + rReg);
        };
        if (lReg.getType() == BasicType.FLOAT || rReg.getType() == BasicType.FLOAT) {
            if (lReg.getType() == BasicType.I32) {
                VReg midReg = new VReg(BasicType.FLOAT, 4);
                irs.add(new RrMIR(RrMIR.Op.MV, midReg, reg1));
                reg1 = midReg;
            }
            if (rReg.getType() == BasicType.I32) {
                VReg midReg = new VReg(BasicType.FLOAT, 4);
                irs.add(new RrMIR(RrMIR.Op.MV, midReg, reg2));
                reg2 = midReg;
            }
            VReg midReg = new VReg(BasicType.I32, 4);
            if (type == BranchVIR.Type.NE) {
                irs.add(new RrrMIR(RrrMIR.Op.EQ, midReg, reg1, reg2));
                irs.add(new BMIR(BMIR.Op.EQ, midReg, MReg.ZERO, trueBlock.getLabel()));
            } else {
                irs.add(new RrrMIR(switch (type) {
                    case EQ -> RrrMIR.Op.EQ;
                    case GE -> RrrMIR.Op.GE;
                    case GT -> RrrMIR.Op.GT;
                    case LE -> RrrMIR.Op.LE;
                    case LT -> RrrMIR.Op.LT;
                    default -> throw new IllegalStateException("Unexpected value: " + type);
                }, midReg, reg1, reg2));
                irs.add(new BMIR(BMIR.Op.NE, midReg, MReg.ZERO, trueBlock.getLabel()));
            }
        } else {
            irs.add(new BMIR(switch (type) {
                case EQ -> BMIR.Op.EQ;
                case GE -> BMIR.Op.GE;
                case GT -> BMIR.Op.GT;
                case LE -> BMIR.Op.LE;
                case LT -> BMIR.Op.LT;
                case NE -> BMIR.Op.NE;
            }, reg1, reg2, trueBlock.getLabel()));
        }
        irs.add(new

                BMIR(null, null, null, falseBlock.getLabel()));
    }

    public static void transBinary(List<MIR> irs, Map<VIR, VReg> virRegMap, BinaryVIR binaryVIR) {
        Value item1 = binaryVIR.left;
        Value item2 = binaryVIR.right;
        if (item1 instanceof VIR ir) {
            if (virRegMap.containsKey(ir))
                item1 = virRegMap.get(ir);
            else {
                VReg reg = new VReg(ir.getType(), ir.getType().getSize());
                virRegMap.put(ir, reg);
                item1 = reg;
            }
        }
        if (item2 instanceof VIR ir) {
            if (virRegMap.containsKey(ir))
                item2 = virRegMap.get(ir);
            else {
                VReg reg = new VReg(ir.getType(), ir.getType().getSize());
                virRegMap.put(ir, reg);
                item2 = reg;
            }
        }
        if (item1 instanceof VReg reg1 && item2 instanceof VReg reg2) {
            MIRBinaryTrans.transBinaryRegReg(irs, binaryVIR, reg1, reg2);
            return;
        }
        if (item1 instanceof VReg reg1 && item2 instanceof InstantValue value2) {
            MIRBinaryTrans.transBinaryRegImm(irs, binaryVIR, reg1, value2);
            return;
        }
        if (item1 instanceof InstantValue value1 && item2 instanceof VReg reg2) {
            MIRBinaryTrans.transBinaryImmReg(irs, binaryVIR, value1, reg2);
            return;
        }
        throw new RuntimeException();
    }

    public static int transCall(List<MIR> irs, Map<VIR, VReg> virRegMap, CallVIR callVIR) {
        List<Value> params = callVIR.params;
        List<MIR> saveCalleeIRs = new ArrayList<>();
        int iSize = 0, fSize = 0;
        for (Value param : params) {
            if (param.getType() == BasicType.FLOAT) {
                if (fSize < MReg.F_CALLER_REGS.size()) {
                    switch (param) {
                        case VIR ir -> {
                            if (virRegMap.containsKey(ir))
                                saveCalleeIRs.add(new RrMIR(RrMIR.Op.MV, MReg.F_CALLER_REGS.get(fSize), virRegMap.get(ir)));
                            else {
                                VReg reg = new VReg(ir.getType(), ir.getType().getSize());
                                saveCalleeIRs.add(new RrMIR(RrMIR.Op.MV, MReg.F_CALLER_REGS.get(fSize), reg));
                                virRegMap.put(ir, reg);
                            }
                        }
                        case VReg reg -> saveCalleeIRs.add(new RrMIR(RrMIR.Op.MV, MReg.F_CALLER_REGS.get(fSize), reg));
                        case InstantValue value ->
                                MIROpHelper.loadImmToReg(saveCalleeIRs, MReg.F_CALLER_REGS.get(fSize), value.floatValue());
                        default -> throw new IllegalStateException("Unexpected value: " + param);
                    }
                } else {
                    switch (param) {
                        case VIR ir -> {
                            if (virRegMap.containsKey(ir))
                                irs.add(new StoreItemMIR(StoreItemMIR.Item.PARAM_CALL, virRegMap.get(ir), (Integer.max(iSize - MReg.I_CALLER_REGS.size(), 0) + Integer.max(fSize - MReg.I_CALLER_REGS.size(), 0)) * 8));
                            else {
                                VReg reg = new VReg(ir.getType(), ir.getType().getSize());
                                irs.add(new StoreItemMIR(StoreItemMIR.Item.PARAM_CALL, reg, (Integer.max(iSize - MReg.I_CALLER_REGS.size(), 0) + Integer.max(fSize - MReg.I_CALLER_REGS.size(), 0)) * 8));
                                virRegMap.put(ir, reg);
                            }
                        }
                        case VReg reg ->
                                irs.add(new StoreItemMIR(StoreItemMIR.Item.PARAM_CALL, reg, (Integer.max(iSize - MReg.I_CALLER_REGS.size(), 0) + Integer.max(fSize - MReg.I_CALLER_REGS.size(), 0)) * 8));
                        case InstantValue value -> {
                            VReg midReg = new VReg(BasicType.I32, 4);
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
                        case VIR ir -> {
                            if (virRegMap.containsKey(ir))
                                saveCalleeIRs.add(new RrMIR(RrMIR.Op.MV, MReg.I_CALLER_REGS.get(iSize), virRegMap.get(ir)));
                            else {
                                VReg reg = new VReg(ir.getType(), ir.getType().getSize());
                                saveCalleeIRs.add(new RrMIR(RrMIR.Op.MV, MReg.I_CALLER_REGS.get(iSize), reg));
                                virRegMap.put(ir, reg);
                            }
                        }
                        case VReg reg -> saveCalleeIRs.add(new RrMIR(RrMIR.Op.MV, MReg.I_CALLER_REGS.get(iSize), reg));
                        case InstantValue value ->
                                MIROpHelper.loadImmToReg(saveCalleeIRs, MReg.I_CALLER_REGS.get(iSize), value.intValue());
                        default -> throw new IllegalStateException("Unexpected value: " + param);
                    }
                } else {
                    switch (param) {
                        case VIR ir -> {
                            if (virRegMap.containsKey(ir))
                                irs.add(new StoreItemMIR(StoreItemMIR.Item.PARAM_CALL, virRegMap.get(ir), (Integer.max(iSize - MReg.I_CALLER_REGS.size(), 0) + Integer.max(fSize - MReg.I_CALLER_REGS.size(), 0)) * 8));
                            else {
                                VReg reg = new VReg(ir.getType(), ir.getType().getSize());
                                irs.add(new StoreItemMIR(StoreItemMIR.Item.PARAM_CALL, reg, (Integer.max(iSize - MReg.I_CALLER_REGS.size(), 0) + Integer.max(fSize - MReg.I_CALLER_REGS.size(), 0)) * 8));
                                virRegMap.put(ir, reg);
                            }
                        }
                        case VReg reg ->
                                irs.add(new StoreItemMIR(StoreItemMIR.Item.PARAM_CALL, reg, (Integer.max(iSize - MReg.I_CALLER_REGS.size(), 0) + Integer.max(fSize - MReg.I_CALLER_REGS.size(), 0)) * 8));
                        case InstantValue value -> {
                            VReg midReg = new VReg(BasicType.I32, 4);
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
        if (callVIR.target != null) {
            if (callVIR.target.getType() == BasicType.FLOAT)
                irs.add(new RrMIR(RrMIR.Op.MV, callVIR.target, MReg.FA0));
            else
                irs.add(new RrMIR(RrMIR.Op.MV, callVIR.target, MReg.A0));
        }
        return params.size();
    }

    public static void transLI(List<MIR> irs, Map<VIR, VReg> virvRegMap, LiVIR liVIR) {
        VReg reg = new VReg(liVIR.getType(), liVIR.getType().getSize());
        virvRegMap.put(liVIR, reg);
        if (reg.getType() == BasicType.I32)
            MIROpHelper.loadImmToReg(irs, reg, liVIR.value.intValue());
        else
            MIROpHelper.loadImmToReg(irs, reg, Float.floatToIntBits(liVIR.value.floatValue()));
    }

    public static void transLoad(List<MIR> irs, Map<VIR, VReg> virRegMap, LoadVIR loadVIR, Map<Symbol, Integer> localOffsets, Map<Symbol, Pair<Boolean, Integer>> paramOffsets) {
        Symbol symbol = loadVIR.symbol;
        if (symbol instanceof GlobalSymbol)
            MIRLoadTrans.transLoadGlobal(irs, virRegMap, loadVIR);
        if (symbol instanceof LocalSymbol)
            MIRLoadTrans.transLoadLocal(irs, virRegMap, loadVIR, localOffsets);
        if (symbol instanceof ParamSymbol)
            MIRLoadTrans.transLoadParam(irs, virRegMap, loadVIR, paramOffsets);
    }

    public static void transMov(Map<VIR, VReg> virRegMap, List<MIR> irs, MovVIR movVIR) {
        switch (movVIR.source) {
            case VIR ir -> {
                if (virRegMap.containsKey(ir))
                    irs.add(new RrMIR(RrMIR.Op.MV, movVIR.target, virRegMap.get(ir)));
                else {
                    VReg reg = new VReg(ir.getType(), ir.getType().getSize());
                    irs.add(new RrMIR(RrMIR.Op.MV, movVIR.target, reg));
                    virRegMap.put(ir, reg);
                }
            }
            case VReg reg -> irs.add(new RrMIR(RrMIR.Op.MV, movVIR.target, reg));
            case InstantValue value -> {
                switch (value.getType()) {
                    case BasicType.I32 -> MIROpHelper.loadImmToReg(irs, movVIR.target, value.intValue());
                    case BasicType.FLOAT -> MIROpHelper.loadImmToReg(irs, movVIR.target, value.floatValue());
                    default -> throw new IllegalStateException("Unexpected value: " + value.getType());
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + movVIR.source);
        }
    }

    public static void transStore(List<MIR> irs, Map<VIR, VReg> virRegMap, StoreVIR storeVIR, Map<Symbol, Integer> localOffsets, Map<Symbol, Pair<Boolean, Integer>> paramOffsets) {
        DataSymbol symbol = storeVIR.symbol;
        if (symbol instanceof GlobalSymbol) {
            MIRStoreTrans.transStoreGlobal(irs, virRegMap, storeVIR);
            return;
        }
        if (symbol instanceof LocalSymbol) {
            MIRStoreTrans.transStoreLocal(irs, virRegMap, storeVIR, localOffsets);
            return;
        }
        if (symbol instanceof ParamSymbol) {
            MIRStoreTrans.transStoreParam(irs, virRegMap, storeVIR, paramOffsets);
            return;
        }
        throw new RuntimeException();
    }

    public static void transUnary(List<MIR> irs, Map<VIR, VReg> virRegMap, UnaryVIR unaryVIR) {
        switch (unaryVIR.source) {
            case VIR ir -> {
                if (virRegMap.containsKey(ir))
                    MIRUnaryTrans.transUnaryReg(irs, unaryVIR, virRegMap.get(ir));
                else {
                    VReg reg = new VReg(ir.getType(), ir.getType().getSize());
                    virRegMap.put(ir, reg);
                    MIRUnaryTrans.transUnaryReg(irs, unaryVIR, reg);
                }
            }
            case VReg reg -> MIRUnaryTrans.transUnaryReg(irs, unaryVIR, reg);
            case InstantValue value -> {
                VReg midReg = new VReg(value.getType(), value.getType().getSize());
                switch (value.getType()) {
                    case BasicType.I32 -> MIROpHelper.loadImmToReg(irs, midReg, value.intValue());
                    case BasicType.FLOAT -> MIROpHelper.loadImmToReg(irs, midReg, value.floatValue());
                    default -> throw new IllegalStateException("Unexpected value: " + value.getType());
                }
                MIRUnaryTrans.transUnaryReg(irs, unaryVIR, midReg);
            }
            default -> throw new IllegalStateException("Unexpected value: " + unaryVIR.source);
        }
    }
}
