package compile.codegen.mirgen.trans;

import compile.codegen.mirgen.mir.*;
import compile.vir.VReg;
import compile.vir.ir.LoadVIR;
import compile.vir.type.BasicType;
import compile.symbol.DataSymbol;
import compile.symbol.Symbol;
import compile.vir.value.Value;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;

public final class MIRLoadTrans {
    private static void ldrRtRsImm(List<MIR> irs, VReg target, VReg source, int imm) {
        if (target.getType() == BasicType.FLOAT) {
            if (imm >= 0 && imm < 1024) {
                irs.add(new LoadMIR(target, source, imm, 4));
                return;
            }
            VReg midReg = new VReg(BasicType.I32, 8);
            MIRBinaryTrans.transAddRegImmI(irs, midReg, source, imm);
            irs.add(new LoadMIR(target, midReg, 0, 4));
        } else {
            if (imm >= -2048 && imm < 2048) {
                irs.add(new LoadMIR(target, source, imm, 4));
                return;
            }
            VReg midReg = new VReg(BasicType.I32, 8);
            MIRBinaryTrans.transAddRegImmI(irs, midReg, source, imm);
            irs.add(new LoadMIR(target, midReg, 0, 4));
        }
    }

    static void transLoadGlobal(List<MIR> irs, LoadVIR loadVIR) {
        DataSymbol symbol = loadVIR.symbol;
        VReg target = loadVIR.target;
        if (symbol.isSingle()) {
            transLoadGlobalSingle(irs, target, symbol);
            return;
        }
        if (loadVIR.indexes.size() != symbol.getDimensionSize()) {
            transLoadGlobalArray(irs, target, loadVIR.indexes, symbol);
            return;
        }
        transLoadGlobalElement(irs, target, loadVIR.indexes, symbol);
    }

    private static void transLoadGlobalArray(List<MIR> irs, VReg target, List<Value> dimensions, DataSymbol symbol) {
        Pair<Integer, List<Pair<VReg, Integer>>> offsetRegDimensions = MIROpHelper.calcDimension(dimensions, symbol.getSizes());
        int offset = offsetRegDimensions.getLeft();
        List<Pair<VReg, Integer>> regDimensions = offsetRegDimensions.getRight();
        if (offset == 0 && regDimensions.isEmpty()) {
            irs.add(new LlaMIR(target, symbol));
            return;
        }
        if (offset == 0) {
            VReg midReg = new VReg(BasicType.I32, 8);
            irs.add(new LlaMIR(midReg, symbol));
            MIROpHelper.addRegDimensionsToReg(irs, target, regDimensions, midReg);
        }
        if (regDimensions.isEmpty()) {
            VReg midReg = new VReg(BasicType.I32, 8);
            irs.add(new LlaMIR(midReg, symbol));
            MIRBinaryTrans.transAddRegImmI(irs, target, midReg, offset);
            return;
        }
        VReg midReg1 = new VReg(BasicType.I32, 8);
        irs.add(new LlaMIR(midReg1, symbol));
        VReg midReg2 = new VReg(BasicType.I32, 8);
        MIROpHelper.addRegDimensionsToReg(irs, midReg2, regDimensions, midReg1);
        MIRBinaryTrans.transAddRegImmI(irs, target, midReg2, offset);
    }

    private static void transLoadGlobalElement(List<MIR> irs, VReg target, List<Value> dimensions, DataSymbol symbol) {
        Pair<Integer, List<Pair<VReg, Integer>>> offsetRegDimensions = MIROpHelper.calcDimension(dimensions, symbol.getSizes());
        int offset = offsetRegDimensions.getLeft();
        List<Pair<VReg, Integer>> regDimensions = offsetRegDimensions.getRight();
        if (regDimensions.isEmpty()) {
            VReg midReg = new VReg(BasicType.I32, 8);
            irs.add(new LlaMIR(midReg, symbol));
            ldrRtRsImm(irs, target, midReg, offset);
            return;
        }
        VReg midReg1 = new VReg(BasicType.I32, 8);
        irs.add(new LlaMIR(midReg1, symbol));
        VReg midReg2 = new VReg(BasicType.I32, 8);
        MIROpHelper.addRegDimensionsToReg(irs, midReg2, regDimensions, midReg1);
        ldrRtRsImm(irs, target, midReg2, offset);
    }

    private static void transLoadGlobalSingle(List<MIR> irs, VReg target, DataSymbol symbol) {
        VReg midReg = new VReg(BasicType.I32, 8);
        irs.add(new LlaMIR(midReg, symbol));
        ldrRtRsImm(irs, target, midReg, 0);
    }

    static void transLoadLocal(List<MIR> irs, LoadVIR loadVIR, Map<Symbol, Integer> localOffsets) {
        DataSymbol symbol = loadVIR.symbol;
        VReg target = loadVIR.target;
        int offset = localOffsets.get(symbol);
        if (symbol.isSingle()) {
            transLoadLocalSingle(irs, target, offset);
            return;
        }
        if (loadVIR.indexes.size() != symbol.getDimensionSize()) {
            transLoadLocalArray(irs, target, loadVIR.indexes, symbol, offset);
            return;
        }
        transLoadLocalElement(irs, target, loadVIR.indexes, symbol, offset);
    }

    private static void transLoadLocalArray(List<MIR> irs, VReg target, List<Value> dimensions, DataSymbol symbol, int offset) {
        Pair<Integer, List<Pair<VReg, Integer>>> offsetRegDimensions = MIROpHelper.calcDimension(dimensions, symbol.getSizes());
        offset += offsetRegDimensions.getLeft();
        List<Pair<VReg, Integer>> regDimensions = offsetRegDimensions.getRight();
        if (regDimensions.isEmpty()) {
            irs.add(new AddRegLocalMIR(target, offset));
            return;
        }
        VReg midReg = new VReg(BasicType.I32, 8);
        irs.add(new AddRegLocalMIR(midReg, offset));
        MIROpHelper.addRegDimensionsToReg(irs, target, regDimensions, midReg);
    }

    private static void transLoadLocalElement(List<MIR> irs, VReg target, List<Value> dimensions, DataSymbol symbol, int offset) {
        Pair<Integer, List<Pair<VReg, Integer>>> offsetRegDimensions = MIROpHelper.calcDimension(dimensions, symbol.getSizes());
        offset += offsetRegDimensions.getLeft();
        List<Pair<VReg, Integer>> regDimensions = offsetRegDimensions.getRight();
        if (regDimensions.isEmpty()) {
            irs.add(new LoadItemMIR(LoadItemMIR.Item.LOCAL, target, offset));
            return;
        }
        VReg midReg1 = new VReg(BasicType.I32, 8);
        irs.add(new AddRegLocalMIR(midReg1, offset));
        VReg midReg2 = new VReg(BasicType.I32, 8);
        MIROpHelper.addRegDimensionsToReg(irs, midReg2, regDimensions, midReg1);
        ldrRtRsImm(irs, target, midReg2, 0);
    }

    private static void transLoadLocalSingle(List<MIR> irs, VReg target, int offset) {
        irs.add(new LoadItemMIR(LoadItemMIR.Item.LOCAL, target, offset));
    }

    static void transLoadParam(List<MIR> irs, LoadVIR loadVIR, Map<Symbol, Pair<Boolean, Integer>> paramOffsets) {
        DataSymbol symbol = loadVIR.symbol;
        VReg target = loadVIR.target;
        Pair<Boolean, Integer> rawOffset = paramOffsets.get(symbol);
        if (symbol.isSingle()) {
            transLoadParamSingle(irs, target, rawOffset.getLeft(), rawOffset.getRight());
            return;
        }
        if (loadVIR.indexes.size() != symbol.getDimensionSize()) {
            transLoadParamArray(irs, target, loadVIR.indexes, symbol, rawOffset.getLeft(), rawOffset.getRight());
            return;
        }
        transLoadParamElement(irs, target, loadVIR.indexes, symbol, rawOffset.getLeft(), rawOffset.getRight());
    }

    private static void transLoadParamArray(List<MIR> irs, VReg target, List<Value> dimensions, DataSymbol symbol, boolean isInner, int offset) {
        Pair<Integer, List<Pair<VReg, Integer>>> offsetRegDimensions = MIROpHelper.calcDimension(dimensions, symbol.getSizes());
        int innerOffset = offsetRegDimensions.getLeft();
        List<Pair<VReg, Integer>> regDimensions = offsetRegDimensions.getRight();
        if (innerOffset == 0 && regDimensions.isEmpty()) {
            irs.add(new LoadItemMIR(isInner ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, target, offset));
            return;
        }
        if (innerOffset == 0) {
            VReg midReg = new VReg(BasicType.I32, 8);
            irs.add(new LoadItemMIR(isInner ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg, offset));
            MIROpHelper.addRegDimensionsToReg(irs, target, regDimensions, midReg);
        }
        if (regDimensions.isEmpty()) {
            VReg midReg = new VReg(BasicType.I32, 8);
            irs.add(new LoadItemMIR(isInner ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg, offset));
            MIRBinaryTrans.transAddRegImmI(irs, target, midReg, innerOffset);
            return;
        }
        VReg midReg1 = new VReg(BasicType.I32, 8);
        irs.add(new LoadItemMIR(isInner ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg1, offset));
        VReg midReg2 = new VReg(BasicType.I32, 8);
        MIROpHelper.addRegDimensionsToReg(irs, midReg2, regDimensions, midReg1);
        MIRBinaryTrans.transAddRegImmI(irs, target, midReg2, innerOffset);
    }

    private static void transLoadParamElement(List<MIR> irs, VReg target, List<Value> dimensions, DataSymbol symbol, boolean isInner, int offset) {
        Pair<Integer, List<Pair<VReg, Integer>>> offsetRegDimensions = MIROpHelper.calcDimension(dimensions, symbol.getSizes());
        int innerOffset = offsetRegDimensions.getLeft();
        List<Pair<VReg, Integer>> regDimensions = offsetRegDimensions.getRight();
        if (regDimensions.isEmpty()) {
            VReg midReg = new VReg(BasicType.I32, 8);
            irs.add(new LoadItemMIR(isInner ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg, offset));
            ldrRtRsImm(irs, target, midReg, innerOffset);
            return;
        }
        VReg midReg1 = new VReg(BasicType.I32, 8);
        irs.add(new LoadItemMIR(isInner ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg1, offset));
        VReg midReg2 = new VReg(BasicType.I32, 8);
        MIROpHelper.addRegDimensionsToReg(irs, midReg2, regDimensions, midReg1);
        ldrRtRsImm(irs, target, midReg2, innerOffset);
    }

    private static void transLoadParamSingle(List<MIR> irs, VReg target, boolean isInner, int offset) {
        irs.add(new LoadItemMIR(isInner ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, target, offset));
    }
}
