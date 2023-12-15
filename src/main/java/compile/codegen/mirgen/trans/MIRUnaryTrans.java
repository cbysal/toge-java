package compile.codegen.mirgen.trans;

import compile.codegen.MReg;
import compile.codegen.mirgen.mir.MIR;
import compile.codegen.mirgen.mir.RrMIR;
import compile.codegen.mirgen.mir.RriMIR;
import compile.codegen.mirgen.mir.RrrMIR;
import compile.codegen.VReg;
import compile.vir.ir.UnaryVIR;
import compile.vir.ir.VIR;
import compile.vir.type.BasicType;

import java.util.List;
import java.util.Map;

public final class MIRUnaryTrans {
    private static void transF2IRegReg(List<MIR> irs, VReg target, VReg source) {
        irs.add(new RrMIR(RrMIR.Op.CVT, target, source));
    }

    private static void transI2FRegReg(List<MIR> irs, VReg target, VReg source) {
        irs.add(new RrMIR(RrMIR.Op.CVT, target, source));
    }

    private static void transLNotRegReg(List<MIR> irs, VReg target, VReg source) {
        if (source.getType() == BasicType.FLOAT) {
            VReg midReg = new VReg(BasicType.FLOAT);
            irs.add(new RrMIR(RrMIR.Op.CVT, midReg, MReg.ZERO));
            irs.add(new RrrMIR(RrrMIR.Op.EQ, target, source, midReg));
        } else {
            irs.add(new RrMIR(RrMIR.Op.SEQZ, target, source));
        }
    }

    private static void transNegRegReg(List<MIR> irs, VReg target, VReg source) {
        irs.add(new RrMIR(RrMIR.Op.NEG, target, source));
    }

    private static void transAbsRegReg(List<MIR> irs, VReg target, VReg source) {
        if (target.getType() == BasicType.FLOAT)
            irs.add(new RrMIR(RrMIR.Op.FABS, target, source));
        else {
            VReg midReg1 = new VReg(BasicType.I32);
            VReg midReg2 = new VReg(BasicType.I32);
            irs.add(new RriMIR(RriMIR.Op.SRAIW, midReg1, source, 31));
            irs.add(new RrrMIR(RrrMIR.Op.XOR, midReg2, source, midReg1));
            irs.add(new RrrMIR(RrrMIR.Op.SUBW, target, midReg2, midReg1));
        }
    }

    static void transUnaryReg(List<MIR> irs, Map<VIR, VReg> virRegMap, UnaryVIR unaryVIR, VReg reg) {
        VReg target = virRegMap.get(unaryVIR);
        switch (unaryVIR.type) {
            case ABS -> transAbsRegReg(irs, target, reg);
            case I2F -> transI2FRegReg(irs, target, reg);
            case FNEG -> transNegRegReg(irs, target, reg);
            default -> throw new RuntimeException();
        }
    }
}
