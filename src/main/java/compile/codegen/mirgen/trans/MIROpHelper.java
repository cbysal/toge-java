package compile.codegen.mirgen.trans;

import compile.codegen.Reg;
import compile.codegen.mirgen.mir.LiMIR;
import compile.codegen.mirgen.mir.MIR;
import compile.codegen.mirgen.mir.MvMIR;
import compile.codegen.mirgen.mir.RrrMIR;
import compile.codegen.virgen.VReg;
import compile.codegen.virgen.vir.VIRItem;
import compile.symbol.Type;
import compile.symbol.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MIROpHelper {
    public static void addRegDimensionsToReg(List<MIR> irs, VReg target, List<Map.Entry<VReg, Integer>> regDimensions
            , VReg source) {
        for (int i = 0; i < regDimensions.size() - 1; i++) {
            Map.Entry<VReg, Integer> regDimension = regDimensions.get(i);
            VReg midReg = new VReg(Type.INT);
            addRtRbRsImm(irs, midReg, source, regDimension.getKey(), regDimension.getValue());
            source = midReg;
        }
        addRtRbRsImm(irs, target, source, regDimensions.get(regDimensions.size() - 1).getKey(),
                regDimensions.get(regDimensions.size() - 1).getValue());
    }

    public static void addRtRbRsImm(List<MIR> irs, VReg target, VReg source1, VReg source2, int imm) {
        VReg midReg1 = new VReg(Type.INT);
        VReg midReg2 = new VReg(Type.INT);
        loadImmToReg(irs, midReg1, imm);
        irs.add(new RrrMIR(RrrMIR.Op.MUL, midReg2, source2, midReg1));
        irs.add(new RrrMIR(RrrMIR.Op.ADD, target, source1, midReg2));
    }

    public static Map.Entry<Integer, List<Map.Entry<VReg, Integer>>> calcDimension(List<VIRItem> dimensions,
                                                                                   int[] sizes) {
        int offset = 0;
        List<Map.Entry<VReg, Integer>> regDimensions = new ArrayList<>();
        for (int i = 0; i < dimensions.size(); i++) {
            VIRItem dimension = dimensions.get(i);
            if (dimension instanceof VReg reg) {
                regDimensions.add(Map.entry(reg, sizes[i]));
                continue;
            }
            if (dimension instanceof Value value) {
                offset += value.getInt() * sizes[i];
                continue;
            }
            throw new RuntimeException();
        }
        return Map.entry(offset, regDimensions);
    }

    public static void loadImmToReg(List<MIR> irs, Reg reg, float imm) {
        if (reg.getType() != Type.FLOAT)
            throw new RuntimeException();
        loadImmToFReg(irs, reg, Float.floatToIntBits(imm));
    }

    public static void loadImmToReg(List<MIR> irs, Reg reg, int imm) {
        if (reg.getType() != Type.INT)
            loadImmToFReg(irs, reg, imm);
        else
            loadImmToIReg(irs, reg, imm);
    }

    private static void loadImmToFReg(List<MIR> irs, Reg reg, int imm) {
        VReg midReg = new VReg(Type.INT);
        loadImmToIReg(irs, midReg, imm);
        irs.add(new MvMIR(reg, midReg));
    }

    private static void loadImmToIReg(List<MIR> irs, Reg reg, int imm) {
        irs.add(new LiMIR(reg, imm));
    }
}
