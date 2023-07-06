package compile.codegen.mirgen.trans;

import common.Pair;
import compile.codegen.mirgen.MReg;
import compile.codegen.mirgen.mir.*;
import compile.codegen.virgen.VReg;
import compile.codegen.virgen.vir.*;
import compile.symbol.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MIROpTrans {
    public static void transB(List<MIR> irs, BVIR bVIR) {
        VIRItem item1 = bVIR.getLeft();
        VIRItem item2 = bVIR.getRight();
        VReg reg1, reg2;
        if (item1 instanceof VReg reg) {
            reg1 = reg;
        } else {
            reg1 = new VReg(item1.getType());
            if (item1.getType() == Type.FLOAT) {
                MIROpHelper.loadImmToReg(irs, reg1, ((Value) item1).getFloat());
            } else {
                MIROpHelper.loadImmToReg(irs, reg1, ((Value) item1).getInt());
            }
        }
        if (item2 instanceof VReg reg) {
            reg2 = reg;
        } else {
            reg2 = new VReg(item2.getType());
            if (item2.getType() == Type.FLOAT) {
                MIROpHelper.loadImmToReg(irs, reg2, ((Value) item2).getFloat());
            } else {
                MIROpHelper.loadImmToReg(irs, reg2, ((Value) item2).getInt());
            }
        }
        if (item1.getType() == Type.FLOAT || item2.getType() == Type.FLOAT) {
            VReg midReg = new VReg(Type.INT);
            if (bVIR.getType() == BVIR.Type.NE) {
                irs.add(new RrrMIR(RrrMIR.Op.EQ, midReg, reg1, reg2));
                irs.add(new BMIR(BMIR.Op.EQ, midReg, MReg.ZERO, bVIR.getBlock().getLabel()));
            } else {
                irs.add(new RrrMIR(switch (bVIR.getType()) {
                    case EQ -> RrrMIR.Op.EQ;
                    case GE -> RrrMIR.Op.GE;
                    case GT -> RrrMIR.Op.GT;
                    case LE -> RrrMIR.Op.LE;
                    case LT -> RrrMIR.Op.LT;
                    default -> throw new IllegalStateException("Unexpected value: " + bVIR.getType());
                }, midReg, reg1, reg2));
                irs.add(new BMIR(BMIR.Op.NE, midReg, MReg.ZERO, bVIR.getBlock().getLabel()));
            }
            return;
        }
        irs.add(new BMIR(switch (bVIR.getType()) {
            case EQ -> BMIR.Op.EQ;
            case GE -> BMIR.Op.GE;
            case GT -> BMIR.Op.GT;
            case LE -> BMIR.Op.LE;
            case LT -> BMIR.Op.LT;
            case NE -> BMIR.Op.NE;
            default -> throw new IllegalStateException("Unexpected value: " + bVIR.getType());
        }, reg1, reg2, bVIR.getBlock().getLabel()));
    }

    public static void transBinary(List<MIR> irs, BinaryVIR binaryVIR) {
        VIRItem item1 = binaryVIR.getLeft();
        VIRItem item2 = binaryVIR.getRight();
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
        List<VIRItem> params = callVIR.getParams();
        List<MIR> saveCalleeIRs = new ArrayList<>();
        int iSize = 0, fSize = 0;
        for (VIRItem param : params) {
            if (param.getType() == Type.FLOAT) {
                if (fSize < MReg.F_CALLER_REGS.size()) {
                    if (param instanceof VReg reg)
                        saveCalleeIRs.add(new MvMIR(MReg.F_CALLER_REGS.get(fSize), reg));
                    else if (param instanceof Value value)
                        MIROpHelper.loadImmToReg(saveCalleeIRs, MReg.F_CALLER_REGS.get(fSize), value.getFloat());
                    else
                        throw new RuntimeException();
                } else {
                    if (param instanceof VReg reg)
                        irs.add(new StoreItemMIR(StoreItemMIR.Item.PARAM_CALL, reg,
                                (Integer.max(iSize - MReg.I_CALLER_REGS.size(), 0) + Integer.max(fSize - MReg.F_CALLER_REGS.size(), 0)) * 8));
                    else if (param instanceof Value value) {
                        VReg midReg = new VReg(Type.INT);
                        MIROpHelper.loadImmToReg(irs, midReg, value.getFloat());
                        irs.add(new StoreItemMIR(StoreItemMIR.Item.PARAM_CALL, midReg,
                                (Integer.max(iSize - MReg.I_CALLER_REGS.size(), 0) + Integer.max(fSize - MReg.F_CALLER_REGS.size(), 0)) * 8));
                    } else
                        throw new RuntimeException();
                }
                fSize++;
            } else {
                if (iSize < MReg.I_CALLER_REGS.size()) {
                    if (param instanceof VReg reg)
                        saveCalleeIRs.add(new MvMIR(MReg.I_CALLER_REGS.get(iSize), reg));
                    else if (param instanceof Value value)
                        MIROpHelper.loadImmToReg(saveCalleeIRs, MReg.I_CALLER_REGS.get(iSize), value.getInt());
                    else
                        throw new RuntimeException();
                } else {
                    if (param instanceof VReg reg)
                        irs.add(new StoreItemMIR(StoreItemMIR.Item.PARAM_CALL, reg,
                                (Integer.max(iSize - MReg.I_CALLER_REGS.size(), 0) + Integer.max(fSize - MReg.F_CALLER_REGS.size(), 0)) * 8));
                    else if (param instanceof Value value) {
                        VReg midReg = new VReg(Type.INT);
                        MIROpHelper.loadImmToReg(irs, midReg, value.getInt());
                        irs.add(new StoreItemMIR(StoreItemMIR.Item.PARAM_CALL, midReg,
                                (Integer.max(iSize - MReg.I_CALLER_REGS.size(), 0) + Integer.max(fSize - MReg.I_CALLER_REGS.size(), 0)) * 8));
                    } else
                        throw new RuntimeException();
                }
                iSize++;
            }
        }
        irs.addAll(saveCalleeIRs);
        irs.add(new BlMIR(callVIR.getFunc()));
        if (callVIR.getRetVal() != null) {
            if (callVIR.getRetVal().getType() == Type.FLOAT)
                irs.add(new MvMIR(callVIR.getRetVal(), MReg.FA0));
            else
                irs.add(new MvMIR(callVIR.getRetVal(), MReg.A0));
        }
        return params.size();
    }

    public static void transLI(List<MIR> irs, LIVIR liVIR) {
        MIROpHelper.loadImmToReg(irs, liVIR.getTarget(), liVIR.getValue());
    }

    public static void transLoad(List<MIR> irs, LoadVIR loadVIR, Map<Symbol, Integer> localOffsets, Map<Symbol,
            Pair<Boolean, Integer>> paramOffsets) {
        Symbol symbol = loadVIR.getSymbol();
        if (symbol instanceof GlobalSymbol globalSymbol) {
            if (globalSymbol.isConst())
                MIRLoadTrans.transLoadConst(irs, loadVIR);
            else
                MIRLoadTrans.transLoadGlobal(irs, loadVIR);
        }
        if (symbol instanceof LocalSymbol)
            MIRLoadTrans.transLoadLocal(irs, loadVIR, localOffsets);
        if (symbol instanceof ParamSymbol)
            MIRLoadTrans.transLoadParam(irs, loadVIR, paramOffsets);
    }

    public static void transMov(List<MIR> irs, MovVIR movVIR) {
        if (movVIR.getTarget().getType() == Type.FLOAT)
            irs.add(new MvMIR(movVIR.getTarget(), movVIR.getSource()));
        else
            irs.add(new MvMIR(movVIR.getTarget(), movVIR.getSource()));
    }

    public static void transStore(List<MIR> irs, StoreVIR storeVIR, Map<Symbol, Integer> localOffsets, Map<Symbol,
            Pair<Boolean, Integer>> paramOffsets) {
        DataSymbol symbol = storeVIR.getSymbol();
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
        VIRItem item = unaryVIR.getSource();
        if (item instanceof VReg reg) {
            MIRUnaryTrans.transUnaryReg(irs, unaryVIR, reg);
            return;
        }
        throw new RuntimeException();
    }
}
