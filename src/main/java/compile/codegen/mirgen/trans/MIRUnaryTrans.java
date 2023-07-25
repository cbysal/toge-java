package compile.codegen.mirgen.trans;

import compile.codegen.mirgen.MReg;
import compile.codegen.mirgen.mir.*;
import compile.codegen.virgen.VReg;
import compile.codegen.virgen.vir.UnaryVIR;
import compile.symbol.Type;

import java.util.List;

public final class MIRUnaryTrans {
    private static void transF2IRegReg(List<MIR> irs, VReg target, VReg source) {
        irs.add(new CvtMIR(target, source));
    }

    private static void transI2FRegReg(List<MIR> irs, VReg target, VReg source) {
        irs.add(new CvtMIR(target, source));
    }

    private static void transLNotRegReg(List<MIR> irs, VReg target, VReg source) {
        if (source.getType() == Type.FLOAT) {
            VReg midReg = new VReg(Type.FLOAT, 4);
            irs.add(new CvtMIR(midReg, MReg.ZERO));
            irs.add(new RrrMIR(RrrMIR.Op.EQ, target, source, midReg));
        } else {
            irs.add(new RriMIR(RriMIR.Op.SLTIU, target, source, 1));
        }
    }

    private static void transNegRegReg(List<MIR> irs, VReg target, VReg source) {
        if (target.getType() == Type.FLOAT)
            irs.add(new NegMIR(target, source));
        else
            irs.add(new RrrMIR(RrrMIR.Op.SUB, target, MReg.ZERO, source));
    }

    private static void transAbsRegReg(List<MIR> irs, VReg target, VReg source) {
        if (target.getType() == Type.FLOAT)
            irs.add(new FabsMIR(target, source));
        else {
            VReg midReg1 = new VReg(Type.INT, 4);
            VReg midReg2 = new VReg(Type.INT, 4);
            irs.add(new RriMIR(RriMIR.Op.SRAIW, midReg1, source, 31));
            irs.add(new RrrMIR(RrrMIR.Op.XOR, midReg2, source, midReg1));
            irs.add(new RrrMIR(RrrMIR.Op.SUBW, target, midReg2, midReg1));
        }
    }

    static void transUnaryReg(List<MIR> irs, UnaryVIR unaryVIR, VReg reg) {
        switch (unaryVIR.type()) {
            case ABS -> transAbsRegReg(irs, unaryVIR.target(), reg);
            case F2I -> transF2IRegReg(irs, unaryVIR.target(), reg);
            case I2F -> transI2FRegReg(irs, unaryVIR.target(), reg);
            case L_NOT -> transLNotRegReg(irs, unaryVIR.target(), reg);
            case NEG -> transNegRegReg(irs, unaryVIR.target(), reg);
            default -> throw new RuntimeException();
        }
    }
}
