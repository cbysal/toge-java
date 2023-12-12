package compile.codegen.mirgen.trans;

import compile.codegen.mirgen.mir.*;
import compile.codegen.VReg;
import compile.vir.contant.ConstantNumber;
import compile.vir.ir.BinaryVIR;
import compile.vir.ir.VIR;
import compile.vir.type.BasicType;

import java.util.List;
import java.util.Map;

public final class MIRBinaryTrans {
    private static void transAddRegImmF(List<MIR> irs, VReg target, VReg source, float imm) {
        VReg midReg = new VReg(BasicType.FLOAT);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transAddRegRegF(irs, target, source, midReg);
    }

    static void transAddRegImmI(List<MIR> irs, VReg target, VReg source, int imm) {
        if (imm >= -2048 && imm < 2048) {
            irs.add(new RriMIR(RriMIR.Op.ADDI, target, source, imm));
            return;
        }
        VReg midReg = new VReg(BasicType.I32);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transAddRegRegI(irs, target, source, midReg);
    }

    private static void transAddRegRegF(List<MIR> irs, VReg target, VReg source1, VReg source2) {
        irs.add(new RrrMIR(RrrMIR.Op.ADD, target, source1, source2));
    }

    private static void transAddRegRegI(List<MIR> irs, VReg target, VReg source1, VReg source2) {
        if (target.getSize() == 4)
            irs.add(new RrrMIR(RrrMIR.Op.ADDW, target, source1, source2));
        else
            irs.add(new RrrMIR(RrrMIR.Op.ADD, target, source1, source2));
    }

    static void transBinaryImmReg(List<MIR> irs, Map<VIR, VReg> virRegMap, BinaryVIR binaryVIR, ConstantNumber value, VReg reg) {
        VReg target = virRegMap.get(binaryVIR);
        switch (binaryVIR.type) {
            case ADD -> {
                if (target.getType() == BasicType.FLOAT)
                    transAddRegImmF(irs, target, reg, value.floatValue());
                else
                    transAddRegImmI(irs, target, reg, value.intValue());
            }
            case DIV -> {
                if (target.getType() == BasicType.FLOAT)
                    transDivImmRegF(irs, target, value.floatValue(), reg);
                else
                    transDivImmRegI(irs, target, value.intValue(), reg);
            }
            case EQ, GE, GT, LE, LT, NE -> {
                BinaryVIR.Type type = switch (binaryVIR.type) {
                    case EQ -> BinaryVIR.Type.EQ;
                    case GE -> BinaryVIR.Type.LT;
                    case GT -> BinaryVIR.Type.LE;
                    case LE -> BinaryVIR.Type.GT;
                    case LT -> BinaryVIR.Type.GE;
                    case NE -> BinaryVIR.Type.NE;
                    default -> throw new RuntimeException();
                };
                if (reg.getType() == BasicType.FLOAT)
                    transCmpRegImmF(irs, type, target, reg, value.floatValue());
                else
                    transCmpRegImmI(irs, type, target, reg, value.intValue());
            }
            case MOD -> transModImmReg(irs, target, value.intValue(), reg);
            case MUL -> {
                if (target.getType() == BasicType.FLOAT)
                    transMulRegImmF(irs, target, reg, value.floatValue());
                else
                    transMulRegImmI(irs, target, reg, value.intValue());
            }
            case SUB -> {
                if (target.getType() == BasicType.FLOAT)
                    transSubImmRegF(irs, target, value.floatValue(), reg);
                else
                    transSubImmRegI(irs, target, value.intValue(), reg);
            }
            default -> throw new RuntimeException();
        }
    }

    static void transBinaryRegImm(List<MIR> irs, Map<VIR, VReg> virRegMap, BinaryVIR binaryVIR, VReg reg, ConstantNumber value) {
        VReg target = virRegMap.get(binaryVIR);
        switch (binaryVIR.type) {
            case ADD -> {
                if (target.getType() == BasicType.FLOAT)
                    transAddRegImmF(irs, target, reg, value.floatValue());
                else
                    transAddRegImmI(irs, target, reg, value.intValue());
            }
            case DIV -> {
                if (target.getType() == BasicType.FLOAT)
                    transDivRegImmF(irs, target, reg, value.floatValue());
                else
                    transDivRegImmI(irs, target, reg, value.intValue());
            }
            case EQ, GE, GT, LE, LT, NE -> {
                if (reg.getType() == BasicType.FLOAT)
                    transCmpRegImmF(irs, binaryVIR.type, target, reg, value.floatValue());
                else
                    transCmpRegImmI(irs, binaryVIR.type, target, reg, value.intValue());
            }
            case MOD -> transModRegImm(irs, target, reg, value.intValue());
            case MUL -> {
                if (target.getType() == BasicType.FLOAT)
                    transMulRegImmF(irs, target, reg, value.floatValue());
                else
                    transMulRegImmI(irs, target, reg, value.intValue());
            }
            case SUB -> {
                if (target.getType() == BasicType.FLOAT)
                    transSubRegImmF(irs, target, reg, value.floatValue());
                else
                    transSubRegImmI(irs, target, reg, value.intValue());
            }
            default -> throw new RuntimeException();
        }
    }

    static void transBinaryRegReg(List<MIR> irs, Map<VIR, VReg> virRegMap, BinaryVIR binaryVIR, VReg reg1, VReg reg2) {
        VReg target = virRegMap.get(binaryVIR);
        switch (binaryVIR.type) {
            case ADD -> {
                if (target.getType() == BasicType.FLOAT)
                    transAddRegRegF(irs, target, reg1, reg2);
                else
                    transAddRegRegI(irs, target, reg1, reg2);
            }
            case DIV -> {
                if (target.getType() == BasicType.FLOAT)
                    transDivRegRegF(irs, target, reg1, reg2);
                else
                    transDivRegRegI(irs, target, reg1, reg2);
            }
            case EQ, GE, GT, LE, LT, NE -> {
                if (reg1.getType() == BasicType.FLOAT)
                    transCmpRegRegF(irs, binaryVIR.type, target, reg1, reg2);
                else
                    transCmpRegRegI(irs, binaryVIR.type, target, reg1, reg2);
            }
            case MOD -> transModRegReg(irs, target, reg1, reg2);
            case MUL -> {
                if (target.getType() == BasicType.FLOAT)
                    transMulRegRegF(irs, target, reg1, reg2);
                else
                    transMulRegRegI(irs, target, reg1, reg2);
            }
            case SUB -> {
                if (target.getType() == BasicType.FLOAT)
                    transSubRegRegF(irs, target, reg1, reg2);
                else
                    transSubRegRegI(irs, target, reg1, reg2);
            }
            default -> throw new RuntimeException();
        }
    }

    private static void transCmpRegImmF(List<MIR> irs, BinaryVIR.Type type, VReg target, VReg source, float imm) {
        VReg midReg = new VReg(BasicType.FLOAT);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transCmpRegRegF(irs, type, target, source, midReg);
    }

    private static void transCmpRegImmI(List<MIR> irs, BinaryVIR.Type type, VReg target, VReg source, int imm) {
        VReg midReg = new VReg(BasicType.I32);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transCmpRegRegI(irs, type, target, source, midReg);
    }

    private static void transCmpRegRegF(List<MIR> irs, BinaryVIR.Type type, VReg target, VReg source1, VReg source2) {
        if (type == BinaryVIR.Type.NE) {
            irs.add(new RrrMIR(RrrMIR.Op.EQ, target, source1, source2));
            irs.add(new RriMIR(RriMIR.Op.XORI, target, target, 1));
            return;
        }
        irs.add(new RrrMIR(switch (type) {
            case EQ -> RrrMIR.Op.EQ;
            case GE -> RrrMIR.Op.GE;
            case GT -> RrrMIR.Op.GT;
            case LE -> RrrMIR.Op.LE;
            case LT -> RrrMIR.Op.LT;
            default -> throw new IllegalStateException("Unexpected value: " + type);
        }, target, source1, source2));
    }

    private static void transCmpRegRegI(List<MIR> irs, BinaryVIR.Type type, VReg target, VReg source1, VReg source2) {
        switch (type) {
            case EQ -> {
                VReg midReg = new VReg(BasicType.I32);
                irs.add(new RrrMIR(RrrMIR.Op.SUB, midReg, source1, source2));
                irs.add(new RrMIR(RrMIR.Op.SEQZ, target, midReg));
            }
            case NE -> {
                VReg midReg = new VReg(BasicType.I32);
                irs.add(new RrrMIR(RrrMIR.Op.SUB, midReg, source1, source2));
                irs.add(new RrMIR(RrMIR.Op.SNEZ, target, midReg));
            }
            case GE -> {
                VReg midReg = new VReg(BasicType.I32);
                irs.add(new RrrMIR(RrrMIR.Op.SLT, midReg, source1, source2));
                irs.add(new RriMIR(RriMIR.Op.XORI, target, midReg, 1));
            }
            case GT -> irs.add(new RrrMIR(RrrMIR.Op.SGT, target, source1, source2));
            case LE -> {
                VReg midReg = new VReg(BasicType.I32);
                irs.add(new RrrMIR(RrrMIR.Op.SGT, midReg, source1, source2));
                irs.add(new RriMIR(RriMIR.Op.XORI, target, midReg, 1));
            }
            case LT -> irs.add(new RrrMIR(RrrMIR.Op.SLT, target, source1, source2));
        }
    }

    private static void transDivImmRegF(List<MIR> irs, VReg target, float imm, VReg source) {
        VReg midReg = new VReg(BasicType.FLOAT);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transDivRegRegF(irs, target, midReg, source);
    }

    private static void transDivImmRegI(List<MIR> irs, VReg target, int imm, VReg source) {
        VReg midReg = new VReg(BasicType.I32);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transDivRegRegI(irs, target, midReg, source);
    }

    private static void transDivRegImmF(List<MIR> irs, VReg target, VReg source, float imm) {
        VReg midReg = new VReg(BasicType.FLOAT);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transDivRegRegF(irs, target, source, midReg);
    }

    private static void transDivRegImmI(List<MIR> irs, VReg target, VReg source, int imm) {
        if (imm == 1) {
            irs.add(new RrMIR(RrMIR.Op.MV, target, source));
            return;
        }
        if (imm == -1) {
            irs.add(new RrMIR(RrMIR.Op.NEG, target, source));
            return;
        }
        int div = imm;
        boolean isPos = true;
        if (div < 0) {
            isPos = false;
            div = Math.abs(div);
        }
        int shift = 0;
        while (1L << (shift + 32) <= (0x7fffffffL - 0x80000000L % div) * (div - (1L << (shift + 32)) % div))
            shift++;
        int magic = (int) ((1L << (shift + 32)) / div + 1);
        VReg midReg1 = new VReg(BasicType.I32);
        VReg midReg2 = new VReg(BasicType.I32);
        VReg midReg3 = new VReg(BasicType.I32);
        VReg midReg4 = new VReg(BasicType.I32);
        MIROpHelper.loadImmToReg(irs, midReg1, magic);
        if (magic >= 0) {
            irs.add(new RrrMIR(RrrMIR.Op.MUL, midReg2, source, midReg1));
            irs.add(new RriMIR(RriMIR.Op.SRLI, midReg2, midReg2, 32));
        } else {
            irs.add(new RrrMIR(RrrMIR.Op.MUL, midReg2, source, midReg1));
            irs.add(new RriMIR(RriMIR.Op.SRLI, midReg2, midReg2, 32));
            irs.add(new RrrMIR(RrrMIR.Op.ADD, midReg2, midReg2, source));
        }
        if (shift != 0)
            irs.add(new RriMIR(RriMIR.Op.SRAIW, midReg3, midReg2, shift));
        else
            midReg3 = midReg2;
        if (isPos) {
            irs.add(new RriMIR(RriMIR.Op.SRLIW, midReg4, source, 31));
            irs.add(new RrrMIR(RrrMIR.Op.ADDW, target, midReg3, midReg4));
        } else {
            irs.add(new RriMIR(RriMIR.Op.SRAIW, midReg4, source, 31));
            irs.add(new RrrMIR(RrrMIR.Op.SUBW, target, midReg3, midReg4));
        }
    }

    private static void transDivRegRegF(List<MIR> irs, VReg target, VReg source1, VReg source2) {
        irs.add(new RrrMIR(RrrMIR.Op.DIV, target, source1, source2));
    }

    private static void transDivRegRegI(List<MIR> irs, VReg target, VReg source1, VReg source2) {
        irs.add(new RrrMIR(RrrMIR.Op.DIVW, target, source1, source2));
    }

    private static void transModImmReg(List<MIR> irs, VReg target, int imm, VReg source) {
        VReg midReg = new VReg(BasicType.I32);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transModRegReg(irs, target, midReg, source);
    }

    private static void transModRegImm(List<MIR> irs, VReg target, VReg source, int imm) {
        if (imm == 1) {
            irs.add(new RrMIR(RrMIR.Op.MV, target, source));
            return;
        }
        if (Integer.bitCount(imm) == 1) {
            VReg midReg1 = new VReg(BasicType.I32);
            if (imm == 2)
                irs.add(new RriMIR(RriMIR.Op.SRLIW, midReg1, source, 31));
            else {
                irs.add(new RriMIR(RriMIR.Op.SRAIW, midReg1, source, 31));
                irs.add(new RriMIR(RriMIR.Op.SRLIW, midReg1, midReg1, Integer.numberOfLeadingZeros(imm) + 1));
            }
            irs.add(new RrrMIR(RrrMIR.Op.ADD, midReg1, midReg1, source));
            if (Math.abs(imm) < 2048) {
                irs.add(new RriMIR(RriMIR.Op.ANDI, midReg1, midReg1, -imm));
            } else {
                VReg midReg2 = new VReg(BasicType.I32);
                irs.add(new LiMIR(midReg2, -imm));
                irs.add(new RrrMIR(RrrMIR.Op.AND, midReg1, midReg1, midReg2));
            }
            irs.add(new RrrMIR(RrrMIR.Op.SUBW, target, source, midReg1));
            return;
        }
        VReg midReg1 = new VReg(BasicType.I32);
        VReg midReg2 = new VReg(BasicType.I32);
        transDivRegImmI(irs, midReg1, source, imm);
        transMulRegImmI(irs, midReg2, midReg1, imm);
        irs.add(new RrrMIR(RrrMIR.Op.SUBW, target, source, midReg2));
    }

    private static void transModRegReg(List<MIR> irs, VReg target, VReg source1, VReg source2) {
        irs.add(new RrrMIR(RrrMIR.Op.REMW, target, source1, source2));
    }

    private static void transMulRegImmF(List<MIR> irs, VReg target, VReg source, float imm) {
        VReg midReg = new VReg(BasicType.FLOAT);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transMulRegRegF(irs, target, source, midReg);
    }

    public static void transMulRegImmI(List<MIR> irs, VReg target, VReg source, int imm) {
        if (imm == 0) {
            MIROpHelper.loadImmToReg(irs, target, 0);
            return;
        }
        if (imm == 1) {
            irs.add(new RrMIR(RrMIR.Op.MV, target, source));
            return;
        }
        if (imm == -1) {
            irs.add(new RrMIR(RrMIR.Op.NEG, target, source));
            return;
        }
        if (Integer.bitCount(imm) == 1) {
            irs.add(new RriMIR(RriMIR.Op.SLLIW, target, source, Integer.numberOfTrailingZeros(imm)));
            return;
        }
        if (Integer.bitCount(imm) == 2 && imm % 2 == 1) {
            VReg midReg = new VReg(BasicType.I32);
            irs.add(new RriMIR(RriMIR.Op.SLLIW, midReg, source, 31 - Integer.numberOfLeadingZeros(imm)));
            irs.add(new RrrMIR(RrrMIR.Op.ADDW, target, midReg, source));
            return;
        }
        if (Integer.numberOfTrailingZeros(imm) == 0 && Integer.numberOfLeadingZeros(imm) + Integer.bitCount(imm) == 32) {
            VReg midReg = new VReg(BasicType.I32);
            irs.add(new RriMIR(RriMIR.Op.SLLIW, midReg, source, 32 - Integer.numberOfLeadingZeros(imm)));
            irs.add(new RrrMIR(RrrMIR.Op.SUBW, target, midReg, source));
            return;
        }
        VReg midReg = new VReg(BasicType.I32);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transMulRegRegI(irs, target, source, midReg);
    }

    private static void transMulRegRegF(List<MIR> irs, VReg target, VReg source1, VReg source2) {
        irs.add(new RrrMIR(RrrMIR.Op.MUL, target, source1, source2));
    }

    private static void transMulRegRegI(List<MIR> irs, VReg target, VReg source1, VReg source2) {
        irs.add(new RrrMIR(RrrMIR.Op.MULW, target, source1, source2));
    }

    private static void transSubImmRegF(List<MIR> irs, VReg target, float imm, VReg source) {
        VReg midReg = new VReg(BasicType.FLOAT);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transSubRegRegF(irs, target, midReg, source);
    }

    private static void transSubImmRegI(List<MIR> irs, VReg target, int imm, VReg source) {
        VReg midReg = new VReg(BasicType.I32);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transSubRegRegI(irs, target, midReg, source);
    }

    private static void transSubRegImmF(List<MIR> irs, VReg target, VReg source, float imm) {
        VReg midReg = new VReg(BasicType.FLOAT);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transSubRegRegF(irs, target, source, midReg);
    }

    private static void transSubRegImmI(List<MIR> irs, VReg target, VReg source, int imm) {
        if (-imm >= -2048 && -imm < 2048) {
            irs.add(new RriMIR(RriMIR.Op.ADDI, target, source, -imm));
            return;
        }
        VReg midReg = new VReg(BasicType.I32);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transSubRegRegI(irs, target, source, midReg);
    }

    private static void transSubRegRegF(List<MIR> irs, VReg target, VReg source1, VReg source2) {
        irs.add(new RrrMIR(RrrMIR.Op.SUB, target, source1, source2));
    }

    private static void transSubRegRegI(List<MIR> irs, VReg target, VReg source1, VReg source2) {
        irs.add(new RrrMIR(RrrMIR.Op.SUBW, target, source1, source2));
    }
}
