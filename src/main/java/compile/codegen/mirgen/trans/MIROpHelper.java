package compile.codegen.mirgen.trans;

import compile.codegen.Reg;
import compile.codegen.VReg;
import compile.codegen.mirgen.mir.LiMIR;
import compile.codegen.mirgen.mir.MIR;
import compile.codegen.mirgen.mir.RrMIR;
import compile.llvm.type.BasicType;

import java.util.List;

public final class MIROpHelper {
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
        VReg midReg = new VReg(BasicType.I32);
        loadImmToIReg(irs, midReg, imm);
        irs.add(new RrMIR(RrMIR.Op.MV, reg, midReg));
    }

    private static void loadImmToIReg(List<MIR> irs, Reg reg, int imm) {
        irs.add(new LiMIR(reg, imm));
    }
}
