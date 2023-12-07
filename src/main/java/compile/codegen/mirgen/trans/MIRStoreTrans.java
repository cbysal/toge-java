package compile.codegen.mirgen.trans;

import compile.codegen.mirgen.mir.*;
import compile.symbol.InstantValue;
import compile.vir.VReg;
import compile.vir.ir.StoreVIR;
import compile.vir.ir.VIR;
import compile.vir.type.BasicType;
import compile.symbol.DataSymbol;
import compile.symbol.Symbol;
import compile.vir.value.Value;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;

public final class MIRStoreTrans {
    private static void strRsRtImm(List<MIR> irs, VReg source, VReg target, int imm) {
        if (imm >= -2048 && imm < 2048) {
            irs.add(new StoreMIR(source, target, imm, 4));
            return;
        }
        VReg midReg = new VReg(BasicType.I32, 8);
        MIRBinaryTrans.transAddRegImmI(irs, midReg, target, imm);
        irs.add(new StoreMIR(source, midReg, 0, 4));
    }

    static void transStoreGlobal(List<MIR> irs, Map<VIR, VReg> virRegMap, StoreVIR storeVIR) {
        DataSymbol symbol = storeVIR.symbol;
        Value source = storeVIR.source;
        if (symbol.isSingle()) {
            transStoreGlobalSingle(irs, virRegMap, source, symbol);
            return;
        }
        transStoreGlobalElement(irs, virRegMap, source, storeVIR.indexes, symbol);
    }

    private static void transStoreGlobalElement(List<MIR> irs, Map<VIR, VReg> virRegMap, Value source, List<Value> dimensions, DataSymbol symbol) {
        Pair<Integer, List<Pair<VReg, Integer>>> offsetRegDimensions = MIROpHelper.calcDimension(virRegMap, dimensions, symbol.getSizes());
        int offset = offsetRegDimensions.getLeft();
        List<Pair<VReg, Integer>> regDimensions = offsetRegDimensions.getRight();
        if (regDimensions.isEmpty()) {
            VReg midReg = new VReg(BasicType.I32, 8);
            irs.add(new LlaMIR(midReg, symbol));
            VReg sourceReg = switch (source) {
                case VIR ir -> {
                    if (virRegMap.containsKey(ir))
                        yield virRegMap.get(ir);
                    VReg reg = new VReg(ir.getType(), ir.getType().getSize());
                    virRegMap.put(ir, reg);
                    yield reg;
                }
                case VReg reg -> reg;
                case InstantValue value -> {
                    VReg midReg1 = new VReg(BasicType.I32, 8);
                    if (symbol.getType() == BasicType.FLOAT) {
                        MIROpHelper.loadImmToReg(irs, midReg1, value.floatValue());
                    } else {
                        MIROpHelper.loadImmToReg(irs, midReg1, value.intValue());
                    }
                    yield midReg1;
                }
                default -> throw new IllegalStateException("Unexpected value: " + source);
            };
            strRsRtImm(irs, sourceReg, midReg, offset);
            return;
        }
        VReg midReg1 = new VReg(BasicType.I32, 8);
        irs.add(new LlaMIR(midReg1, symbol));
        VReg midReg2 = new VReg(BasicType.I32, 8);
        MIROpHelper.addRegDimensionsToReg(irs, midReg2, regDimensions, midReg1);
        VReg sourceReg = switch (source) {
            case VIR ir -> {
                if (virRegMap.containsKey(ir)) {
                    yield virRegMap.get(ir);
                }
                VReg reg = new VReg(ir.getType(), ir.getType().getSize());
                virRegMap.put(ir, reg);
                yield reg;
            }
            case VReg reg -> reg;
            case InstantValue value -> {
                VReg midReg3 = new VReg(BasicType.I32, 8);
                if (symbol.getType() == BasicType.FLOAT) {
                    MIROpHelper.loadImmToReg(irs, midReg3, value.floatValue());
                } else {
                    MIROpHelper.loadImmToReg(irs, midReg3, value.intValue());
                }
                yield midReg3;
            }
            default -> throw new IllegalStateException("Unexpected value: " + source);
        };
        strRsRtImm(irs, sourceReg, midReg2, offset);
    }

    private static void transStoreGlobalSingle(List<MIR> irs, Map<VIR, VReg> virRegMap, Value source, DataSymbol symbol) {
        VReg midReg = new VReg(BasicType.I32, 8);
        irs.add(new LlaMIR(midReg, symbol));
        switch (source) {
            case VIR ir -> {
                if (virRegMap.containsKey(ir)) {
                    irs.add(new StoreMIR(virRegMap.get(ir), midReg, 0, 4));
                    return;
                }
                VReg reg = new VReg(ir.getType(), ir.getType().getSize());
                virRegMap.put(ir, reg);
                irs.add(new StoreMIR(reg, midReg, 0, 4));
            }
            case VReg reg -> irs.add(new StoreMIR(reg, midReg, 0, 4));
            case InstantValue value -> {
                if (symbol.getType() == BasicType.FLOAT) {
                    irs.add(new LiMIR(midReg, Float.floatToIntBits(value.floatValue())));
                } else {
                    irs.add(new LiMIR(midReg, value.intValue()));
                }
                irs.add(new StoreMIR(midReg, midReg, 0, 4));
            }
            default -> throw new IllegalStateException("Unexpected value: " + source);
        }
    }

    static void transStoreLocal(List<MIR> irs, Map<VIR, VReg> virRegMap, StoreVIR storeVIR, Map<Symbol, Integer> localOffsets) {
        DataSymbol symbol = storeVIR.symbol;
        Value source = storeVIR.source;
        int offset = localOffsets.get(symbol);
        if (storeVIR.isSingle()) {
            transStoreLocalSingle(irs, virRegMap, source, offset);
            return;
        }
        transStoreLocalElement(irs, virRegMap, source, storeVIR.indexes, symbol, offset);
    }

    private static void transStoreLocalElement(List<MIR> irs, Map<VIR, VReg> virRegMap, Value source, List<Value> dimensions, DataSymbol symbol, int offset) {
        Pair<Integer, List<Pair<VReg, Integer>>> offsetRegDimensions = MIROpHelper.calcDimension(virRegMap, dimensions, symbol.getSizes());
        offset += offsetRegDimensions.getLeft();
        List<Pair<VReg, Integer>> regDimensions = offsetRegDimensions.getRight();
        if (regDimensions.isEmpty()) {
            VReg sourceReg = switch (source) {
                case VIR ir -> {
                    if (virRegMap.containsKey(ir))
                        yield virRegMap.get(ir);
                    VReg reg = new VReg(ir.getType(), ir.getType().getSize());
                    virRegMap.put(ir, reg);
                    yield reg;
                }
                case VReg reg -> reg;
                case InstantValue value -> {
                    VReg midReg = new VReg(BasicType.I32, 8);
                    if (source.getType() == BasicType.FLOAT) {
                        MIROpHelper.loadImmToReg(irs, midReg, value.floatValue());
                    } else {
                        MIROpHelper.loadImmToReg(irs, midReg, value.intValue());
                    }
                    yield midReg;
                }
                default -> throw new IllegalStateException("Unexpected value: " + source);
            };
            irs.add(new StoreItemMIR(StoreItemMIR.Item.LOCAL, sourceReg, offset));
            return;
        }
        VReg midReg1 = new VReg(BasicType.I32, 8);
        irs.add(new AddRegLocalMIR(midReg1, offset));
        VReg midReg2 = new VReg(BasicType.I32, 8);
        MIROpHelper.addRegDimensionsToReg(irs, midReg2, regDimensions, midReg1);
        VReg sourceReg = switch (source) {
            case VIR ir -> {
                if (virRegMap.containsKey(ir)) {
                    yield virRegMap.get(ir);
                }
                VReg reg = new VReg(ir.getType(), ir.getType().getSize());
                virRegMap.put(ir, reg);
                yield reg;
            }
            case VReg reg -> reg;
            case InstantValue value -> {
                VReg midReg3 = new VReg(BasicType.I32, 8);
                if (source.getType() == BasicType.FLOAT) {
                    MIROpHelper.loadImmToReg(irs, midReg3, value.floatValue());
                } else {
                    MIROpHelper.loadImmToReg(irs, midReg3, value.intValue());
                }
                yield midReg3;
            }
            default -> throw new IllegalStateException("Unexpected value: " + source);
        };
        strRsRtImm(irs, sourceReg, midReg2, 0);
    }

    private static void transStoreLocalSingle(List<MIR> irs, Map<VIR, VReg> virRegMap, Value source, int offset) {
        VReg sourceReg = switch (source) {
            case VIR ir -> {
                if (virRegMap.containsKey(ir))
                    yield virRegMap.get(ir);
                VReg reg = new VReg(ir.getType(), ir.getType().getSize());
                virRegMap.put(ir, reg);
                yield reg;
            }
            case VReg reg -> reg;
            case InstantValue value -> {
                VReg midReg = new VReg(BasicType.I32, 8);
                if (source.getType() == BasicType.FLOAT) {
                    MIROpHelper.loadImmToReg(irs, midReg, value.floatValue());
                } else {
                    MIROpHelper.loadImmToReg(irs, midReg, value.intValue());
                }
                yield midReg;
            }
            default -> throw new IllegalStateException("Unexpected value: " + source);
        };
        irs.add(new StoreItemMIR(StoreItemMIR.Item.LOCAL, sourceReg, offset));
    }

    static void transStoreParam(List<MIR> irs, Map<VIR, VReg> virRegMap, StoreVIR storeVIR, Map<Symbol, Pair<Boolean, Integer>> paramOffsets) {
        DataSymbol symbol = storeVIR.symbol;
        Value source = storeVIR.source;
        Pair<Boolean, Integer> rawOffset = paramOffsets.get(symbol);
        if (symbol.isSingle()) {
            transStoreParamSingle(irs, virRegMap, source, rawOffset.getLeft(), rawOffset.getRight());
            return;
        }
        transStoreParamElement(irs, virRegMap, source, storeVIR.indexes, symbol, rawOffset.getLeft(), rawOffset.getRight());
    }

    private static void transStoreParamElement(List<MIR> irs, Map<VIR, VReg> virRegMap, Value source, List<Value> dimensions, DataSymbol symbol, Boolean isInner, Integer offset) {
        Pair<Integer, List<Pair<VReg, Integer>>> offsetRegDimensions = MIROpHelper.calcDimension(virRegMap, dimensions, symbol.getSizes());
        int innerOffset = offsetRegDimensions.getLeft();
        List<Pair<VReg, Integer>> regDimensions = offsetRegDimensions.getRight();
        VReg midReg = new VReg(BasicType.I32, 8);
        irs.add(new LoadItemMIR(isInner ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg, offset));
        for (Pair<VReg, Integer> regDimension : regDimensions) {
            VReg midReg1 = new VReg(BasicType.I32, 8);
            MIROpHelper.addRtRbRsImm(irs, midReg1, midReg, regDimension.getLeft(), regDimension.getRight());
            midReg = midReg1;
        }
        VReg sourceReg = switch (source) {
            case VIR ir -> {
                if (virRegMap.containsKey(ir)) {
                    yield virRegMap.get(ir);
                }
                VReg reg = new VReg(ir.getType(), ir.getType().getSize());
                virRegMap.put(ir, reg);
                yield reg;
            }
            case VReg reg -> reg;
            case InstantValue value -> {
                VReg midReg2 = new VReg(BasicType.I32, 8);
                if (source.getType() == BasicType.FLOAT) {
                    MIROpHelper.loadImmToReg(irs, midReg2, value.floatValue());
                } else {
                    MIROpHelper.loadImmToReg(irs, midReg2, value.intValue());
                }
                yield midReg2;
            }
            default -> throw new IllegalStateException("Unexpected value: " + source);
        };
        strRsRtImm(irs, sourceReg, midReg, innerOffset);
    }

    private static void transStoreParamSingle(List<MIR> irs, Map<VIR, VReg> virRegMap, Value source, Boolean isInner, Integer offset) {
        VReg sourceReg = switch (source) {
            case VIR ir -> {
                if (virRegMap.containsKey(ir))
                    yield virRegMap.get(ir);
                VReg reg = new VReg(ir.getType(), ir.getType().getSize());
                virRegMap.put(ir, reg);
                yield reg;
            }
            case VReg reg -> reg;
            case InstantValue value -> {
                VReg midReg = new VReg(BasicType.I32, 8);
                if (source.getType() == BasicType.FLOAT) {
                    MIROpHelper.loadImmToReg(irs, midReg, value.floatValue());
                } else {
                    MIROpHelper.loadImmToReg(irs, midReg, value.intValue());
                }
                yield midReg;
            }
            default -> throw new IllegalStateException("Unexpected value: " + source);
        };
        irs.add(new StoreItemMIR(isInner ? StoreItemMIR.Item.PARAM_INNER : StoreItemMIR.Item.PARAM_OUTER, sourceReg, offset));
    }
}
