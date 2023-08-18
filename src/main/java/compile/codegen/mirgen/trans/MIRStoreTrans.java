package compile.codegen.mirgen.trans;

import common.Pair;
import compile.codegen.mirgen.mir.*;
import compile.codegen.virgen.VReg;
import compile.codegen.virgen.vir.StoreVIR;
import compile.codegen.virgen.vir.VIRItem;
import compile.symbol.DataSymbol;
import compile.symbol.Symbol;
import compile.symbol.Type;

import java.util.List;
import java.util.Map;

public final class MIRStoreTrans {
    private static void strRsRtImm(List<MIR> irs, VReg source, VReg target, int imm) {
        if (imm >= -2048 && imm < 2048) {
            irs.add(new StoreMIR(source, target, imm, 4));
            return;
        }
        VReg midReg = new VReg(Type.INT, 8);
        MIRBinaryTrans.transAddRegImmI(irs, midReg, target, imm);
        irs.add(new StoreMIR(source, midReg, 0, 4));
    }

    static void transStoreGlobal(List<MIR> irs, StoreVIR storeVIR) {
        DataSymbol symbol = storeVIR.symbol;
        VReg source = storeVIR.source;
        if (symbol.isSingle()) {
            transStoreGlobalSingle(irs, source, symbol);
            return;
        }
        transStoreGlobalElement(irs, source, storeVIR.indexes, symbol);
    }

    private static void transStoreGlobalElement(List<MIR> irs, VReg source, List<VIRItem> dimensions,
                                                DataSymbol symbol) {
        Pair<Integer, List<Pair<VReg, Integer>>> offsetRegDimensions = MIROpHelper.calcDimension(dimensions,
                symbol.getSizes());
        int offset = offsetRegDimensions.first();
        List<Pair<VReg, Integer>> regDimensions = offsetRegDimensions.second();
        if (regDimensions.isEmpty()) {
            VReg midReg = new VReg(Type.INT, 8);
            irs.add(new LlaMIR(midReg, symbol));
            strRsRtImm(irs, source, midReg, offset);
            return;
        }
        VReg midReg1 = new VReg(Type.INT, 8);
        irs.add(new LlaMIR(midReg1, symbol));
        VReg midReg2 = new VReg(Type.INT, 8);
        MIROpHelper.addRegDimensionsToReg(irs, midReg2, regDimensions, midReg1);
        strRsRtImm(irs, source, midReg2, offset);
    }

    private static void transStoreGlobalSingle(List<MIR> irs, VReg source, DataSymbol symbol) {
        VReg midReg = new VReg(Type.INT, 8);
        irs.add(new LlaMIR(midReg, symbol));
        irs.add(new StoreMIR(source, midReg, 0, 4));
    }

    static void transStoreLocal(List<MIR> irs, StoreVIR storeVIR, Map<Symbol, Integer> localOffsets) {
        DataSymbol symbol = storeVIR.symbol;
        VReg source = storeVIR.source;
        int offset = localOffsets.get(symbol);
        if (storeVIR.isSingle()) {
            transStoreLocalSingle(irs, source, offset);
            return;
        }
        transStoreLocalElement(irs, source, storeVIR.indexes, symbol, offset);
    }

    private static void transStoreLocalElement(List<MIR> irs, VReg source, List<VIRItem> dimensions,
                                               DataSymbol symbol, int offset) {
        Pair<Integer, List<Pair<VReg, Integer>>> offsetRegDimensions = MIROpHelper.calcDimension(dimensions,
                symbol.getSizes());
        offset += offsetRegDimensions.first();
        List<Pair<VReg, Integer>> regDimensions = offsetRegDimensions.second();
        if (regDimensions.isEmpty()) {
            irs.add(new StoreItemMIR(StoreItemMIR.Item.LOCAL, source, offset));
            return;
        }
        VReg midReg1 = new VReg(Type.INT, 8);
        irs.add(new AddRegLocalMIR(midReg1, offset));
        VReg midReg2 = new VReg(Type.INT, 8);
        MIROpHelper.addRegDimensionsToReg(irs, midReg2, regDimensions, midReg1);
        strRsRtImm(irs, source, midReg2, 0);
    }

    private static void transStoreLocalSingle(List<MIR> irs, VReg source, int offset) {
        irs.add(new StoreItemMIR(StoreItemMIR.Item.LOCAL, source, offset));
    }

    static void transStoreParam(List<MIR> irs, StoreVIR storeVIR, Map<Symbol, Pair<Boolean, Integer>> paramOffsets) {
        DataSymbol symbol = storeVIR.symbol;
        VReg source = storeVIR.source;
        Pair<Boolean, Integer> rawOffset = paramOffsets.get(symbol);
        if (symbol.isSingle()) {
            transStoreParamSingle(irs, source, rawOffset.first(), rawOffset.second());
            return;
        }
        transStoreParamElement(irs, source, storeVIR.indexes, symbol, rawOffset.first(), rawOffset.second());
    }

    private static void transStoreParamElement(List<MIR> irs, VReg source, List<VIRItem> dimensions,
                                               DataSymbol symbol, Boolean isInner, Integer offset) {
        Pair<Integer, List<Pair<VReg, Integer>>> offsetRegDimensions = MIROpHelper.calcDimension(dimensions,
                symbol.getSizes());
        int innerOffset = offsetRegDimensions.first();
        List<Pair<VReg, Integer>> regDimensions = offsetRegDimensions.second();
        VReg midReg = new VReg(Type.INT, 8);
        irs.add(new LoadItemMIR(isInner ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg, offset));
        for (Pair<VReg, Integer> regDimension : regDimensions) {
            VReg midReg1 = new VReg(Type.INT, 8);
            MIROpHelper.addRtRbRsImm(irs, midReg1, midReg, regDimension.first(), regDimension.second());
            midReg = midReg1;
        }
        strRsRtImm(irs, source, midReg, innerOffset);
    }

    private static void transStoreParamSingle(List<MIR> irs, VReg source, Boolean isInner, Integer offset) {
        irs.add(new StoreItemMIR(isInner ? StoreItemMIR.Item.PARAM_INNER : StoreItemMIR.Item.PARAM_OUTER, source,
                offset));
    }
}
