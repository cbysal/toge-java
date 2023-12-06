package compile.codegen.mirgen.trans;

import compile.codegen.mirgen.MReg;
import compile.codegen.mirgen.mir.*;
import compile.vir.Block;
import compile.vir.VReg;
import compile.vir.ir.*;
import compile.vir.type.BasicType;
import compile.symbol.*;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MIROpTrans {
    public static void transBranch(List<MIR> irs, BranchVIR branchVIR) {
        BranchVIR.Type type = branchVIR.type;
        VIRItem lReg = branchVIR.left;
        VIRItem rReg = branchVIR.right;
        Block trueBlock = branchVIR.trueBlock;
        Block falseBlock = branchVIR.falseBlock;
        VReg reg1, reg2;
        if (lReg instanceof VReg reg) {
            reg1 = reg;
        } else {
            reg1 = new VReg(lReg.getType(), 4);
            if (lReg.getType() == BasicType.FLOAT) {
                MIROpHelper.loadImmToReg(irs, reg1, ((Value) lReg).floatValue());
            } else {
                MIROpHelper.loadImmToReg(irs, reg1, ((Value) lReg).intValue());
            }
        }
        if (rReg instanceof VReg reg) {
            reg2 = reg;
        } else {
            reg2 = new VReg(rReg.getType(), 4);
            if (rReg.getType() == BasicType.FLOAT) {
                MIROpHelper.loadImmToReg(irs, reg2, ((Value) rReg).floatValue());
            } else {
                MIROpHelper.loadImmToReg(irs, reg2, ((Value) rReg).intValue());
            }
        }
        if (lReg.getType() == BasicType.FLOAT || rReg.getType() == BasicType.FLOAT) {
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
        irs.add(new BMIR(null, null, null, falseBlock.getLabel()));
    }

    public static void transBinary(List<MIR> irs, BinaryVIR binaryVIR) {
        VIRItem item1 = binaryVIR.left;
        VIRItem item2 = binaryVIR.right;
        if (item1 instanceof VReg reg1 && item2 instanceof VReg reg2) {
            MIRBinaryTrans.transBinaryRegReg(irs, binaryVIR, reg1, reg2);
            return;
        }
        if (item1 instanceof VReg reg1 && item2 instanceof Value value2) {
            MIRBinaryTrans.transBinaryRegImm(irs, binaryVIR, reg1, value2);
            return;
        }
        if (item1 instanceof Value value1 && item2 instanceof VReg reg2) {
            MIRBinaryTrans.transBinaryImmReg(irs, binaryVIR, value1, reg2);
            return;
        }
        throw new RuntimeException();
    }

    public static int transCall(List<MIR> irs, CallVIR callVIR) {
        List<VIRItem> params = callVIR.params;
        List<MIR> saveCalleeIRs = new ArrayList<>();
        int iSize = 0, fSize = 0;
        for (VIRItem param : params) {
            if (param.getType() == BasicType.FLOAT) {
                if (fSize < MReg.F_CALLER_REGS.size()) {
                    if (param instanceof VReg reg)
                        saveCalleeIRs.add(new RrMIR(RrMIR.Op.MV, MReg.F_CALLER_REGS.get(fSize), reg));
                    else if (param instanceof Value value)
                        MIROpHelper.loadImmToReg(saveCalleeIRs, MReg.F_CALLER_REGS.get(fSize), value.floatValue());
                    else
                        throw new RuntimeException();
                } else {
                    if (param instanceof VReg reg)
                        irs.add(new StoreItemMIR(StoreItemMIR.Item.PARAM_CALL, reg, (Integer.max(iSize - MReg.I_CALLER_REGS.size(), 0) + Integer.max(fSize - MReg.F_CALLER_REGS.size(), 0)) * 8));
                    else if (param instanceof Value value) {
                        VReg midReg = new VReg(BasicType.I32, 4);
                        MIROpHelper.loadImmToReg(irs, midReg, value.floatValue());
                        irs.add(new StoreItemMIR(StoreItemMIR.Item.PARAM_CALL, midReg, (Integer.max(iSize - MReg.I_CALLER_REGS.size(), 0) + Integer.max(fSize - MReg.F_CALLER_REGS.size(), 0)) * 8));
                    } else
                        throw new RuntimeException();
                }
                fSize++;
            } else {
                if (iSize < MReg.I_CALLER_REGS.size()) {
                    if (param instanceof VReg reg)
                        saveCalleeIRs.add(new RrMIR(RrMIR.Op.MV, MReg.I_CALLER_REGS.get(iSize), reg));
                    else if (param instanceof Value value)
                        MIROpHelper.loadImmToReg(saveCalleeIRs, MReg.I_CALLER_REGS.get(iSize), value.intValue());
                    else
                        throw new RuntimeException();
                } else {
                    if (param instanceof VReg reg)
                        irs.add(new StoreItemMIR(StoreItemMIR.Item.PARAM_CALL, reg, (Integer.max(iSize - MReg.I_CALLER_REGS.size(), 0) + Integer.max(fSize - MReg.F_CALLER_REGS.size(), 0)) * 8));
                    else if (param instanceof Value value) {
                        VReg midReg = new VReg(BasicType.I32, 4);
                        MIROpHelper.loadImmToReg(irs, midReg, value.intValue());
                        irs.add(new StoreItemMIR(StoreItemMIR.Item.PARAM_CALL, midReg, (Integer.max(iSize - MReg.I_CALLER_REGS.size(), 0) + Integer.max(fSize - MReg.I_CALLER_REGS.size(), 0)) * 8));
                    } else
                        throw new RuntimeException();
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

    public static void transLI(List<MIR> irs, LiVIR liVIR) {
        if (liVIR.target.getType() == BasicType.I32)
            MIROpHelper.loadImmToReg(irs, liVIR.target, liVIR.value.intValue());
        else
            MIROpHelper.loadImmToReg(irs, liVIR.target, Float.floatToIntBits(liVIR.value.floatValue()));
    }

    public static void transLoad(List<MIR> irs, LoadVIR loadVIR, Map<Symbol, Integer> localOffsets, Map<Symbol, Pair<Boolean, Integer>> paramOffsets) {
        Symbol symbol = loadVIR.symbol;
        if (symbol instanceof GlobalSymbol)
            MIRLoadTrans.transLoadGlobal(irs, loadVIR);
        if (symbol instanceof LocalSymbol)
            MIRLoadTrans.transLoadLocal(irs, loadVIR, localOffsets);
        if (symbol instanceof ParamSymbol)
            MIRLoadTrans.transLoadParam(irs, loadVIR, paramOffsets);
    }

    public static void transMov(List<MIR> irs, MovVIR movVIR) {
        if (movVIR.target.getType() == BasicType.FLOAT)
            irs.add(new RrMIR(RrMIR.Op.MV, movVIR.target, movVIR.source));
        else
            irs.add(new RrMIR(RrMIR.Op.MV, movVIR.target, movVIR.source));
    }

    public static void transStore(List<MIR> irs, StoreVIR storeVIR, Map<Symbol, Integer> localOffsets, Map<Symbol, Pair<Boolean, Integer>> paramOffsets) {
        DataSymbol symbol = storeVIR.symbol;
        if (symbol instanceof GlobalSymbol) {
            MIRStoreTrans.transStoreGlobal(irs, storeVIR);
            return;
        }
        if (symbol instanceof LocalSymbol) {
            MIRStoreTrans.transStoreLocal(irs, storeVIR, localOffsets);
            return;
        }
        if (symbol instanceof ParamSymbol) {
            MIRStoreTrans.transStoreParam(irs, storeVIR, paramOffsets);
            return;
        }
        throw new RuntimeException();
    }

    public static void transUnary(List<MIR> irs, UnaryVIR unaryVIR) {
        VIRItem item = unaryVIR.source;
        if (item instanceof VReg reg) {
            MIRUnaryTrans.transUnaryReg(irs, unaryVIR, reg);
            return;
        }
        throw new RuntimeException();
    }
}
