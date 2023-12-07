package compile.codegen.mirgen.trans;

import compile.codegen.Reg;
import compile.codegen.mirgen.mir.LiMIR;
import compile.codegen.mirgen.mir.MIR;
import compile.codegen.mirgen.mir.RrMIR;
import compile.codegen.mirgen.mir.RrrMIR;
import compile.codegen.VReg;
import compile.vir.ir.VIR;
import compile.vir.type.BasicType;
import compile.symbol.InstantValue;
import compile.vir.value.Value;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MIROpHelper {
    public static void addRegDimensionsToReg(List<MIR> irs, VReg target, List<Pair<VReg, Integer>> regDimensions, VReg source) {
        for (int i = 0; i < regDimensions.size() - 1; i++) {
            Pair<VReg, Integer> regDimension = regDimensions.get(i);
            VReg midReg = new VReg(BasicType.I32, 8);
            addRtRbRsImm(irs, midReg, source, regDimension.getLeft(), regDimension.getRight());
            source = midReg;
        }
        addRtRbRsImm(irs, target, source, regDimensions.get(regDimensions.size() - 1).getLeft(), regDimensions.get(regDimensions.size() - 1).getRight());
    }

    public static void addRtRbRsImm(List<MIR> irs, VReg target, VReg source1, VReg source2, int imm) {
        VReg midReg = new VReg(BasicType.I32, 4);
        MIRBinaryTrans.transMulRegImmI(irs, midReg, source2, imm);
        irs.add(new RrrMIR(RrrMIR.Op.ADD, target, source1, midReg));
    }

    public static Pair<Integer, List<Pair<VReg, Integer>>> calcDimension(Map<VIR, VReg> virRegMap, List<Value> dimensions, int[] sizes) {
        int offset = 0;
        List<Pair<VReg, Integer>> regDimensions = new ArrayList<>();
        for (int i = 0; i < dimensions.size(); i++) {
            Value dimension = dimensions.get(i);
            if (dimension instanceof VIR ir) {
                if (virRegMap.containsKey(ir))
                    regDimensions.add(Pair.of(virRegMap.get(ir), sizes[i]));
                else {
                    VReg midReg = new VReg(ir.getType(), ir.getType().getSize());
                    virRegMap.put(ir, midReg);
                    regDimensions.add(Pair.of(midReg, sizes[i]));
                }
                continue;
            }
            if (dimension instanceof VReg reg) {
                regDimensions.add(Pair.of(reg, sizes[i]));
                continue;
            }
            if (dimension instanceof InstantValue value) {
                offset += value.intValue() * sizes[i];
                continue;
            }
            throw new RuntimeException();
        }
        return Pair.of(offset, regDimensions);
    }

    public static void loadImmToReg(List<MIR> irs, Reg reg, float imm) {
        if (reg.getType() != BasicType.FLOAT)
            throw new RuntimeException();
        loadImmToFReg(irs, reg, Float.floatToIntBits(imm));
    }

    public static void loadImmToReg(List<MIR> irs, Reg reg, int imm) {
        if (reg.getType() != BasicType.I32)
            loadImmToFReg(irs, reg, imm);
        else
            loadImmToIReg(irs, reg, imm);
    }

    private static void loadImmToFReg(List<MIR> irs, Reg reg, int imm) {
        VReg midReg = new VReg(BasicType.I32, 4);
        loadImmToIReg(irs, midReg, imm);
        irs.add(new RrMIR(RrMIR.Op.MV, reg, midReg));
    }

    private static void loadImmToIReg(List<MIR> irs, Reg reg, int imm) {
        irs.add(new LiMIR(reg, imm));
    }
}
