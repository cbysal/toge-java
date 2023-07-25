package compile.codegen.mirgen.trans;

import compile.codegen.mirgen.mir.*;
import compile.codegen.virgen.VReg;
import compile.codegen.virgen.vir.BinaryVIR;
import compile.symbol.Type;
import compile.symbol.Value;

import java.util.List;

public final class MIRBinaryTrans {
    private static void transAddRegImmF(List<MIR> irs, VReg target, VReg source, float imm) {
        VReg midReg = new VReg(Type.FLOAT, 4);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transAddRegRegF(irs, target, source, midReg);
    }

    static void transAddRegImmI(List<MIR> irs, VReg target, VReg source, int imm) {
        if (imm >= -2048 && imm < 2048) {
            irs.add(new RriMIR(RriMIR.Op.ADDI, target, source, imm));
            return;
        }
        VReg midReg = new VReg(Type.INT, 4);
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

    static void transBinaryImmReg(List<MIR> irs, BinaryVIR binaryVIR, Value value, VReg reg) {
        switch (binaryVIR.type()) {
            case ADD -> {
                if (binaryVIR.target().getType() == Type.FLOAT)
                    transAddRegImmF(irs, binaryVIR.target(), reg, value.getFloat());
                else
                    transAddRegImmI(irs, binaryVIR.target(), reg, value.getInt());
            }
            case DIV -> {
                if (binaryVIR.target().getType() == Type.FLOAT)
                    transDivImmRegF(irs, binaryVIR.target(), value.getFloat(), reg);
                else
                    transDivImmRegI(irs, binaryVIR.target(), value.getInt(), reg);
            }
            case EQ, GE, GT, LE, LT, NE -> {
                BinaryVIR.Type type = switch (binaryVIR.type()) {
                    case EQ -> BinaryVIR.Type.EQ;
                    case GE -> BinaryVIR.Type.LT;
                    case GT -> BinaryVIR.Type.LE;
                    case LE -> BinaryVIR.Type.GT;
                    case LT -> BinaryVIR.Type.GE;
                    case NE -> BinaryVIR.Type.NE;
                    default -> throw new RuntimeException();
                };
                if (reg.getType() == Type.FLOAT)
                    transCmpRegImmF(irs, type, binaryVIR.target(), reg, value.getFloat());
                else
                    transCmpRegImmI(irs, type, binaryVIR.target(), reg, value.getInt());
            }
            case MOD -> transModImmReg(irs, binaryVIR.target(), value.getInt(), reg);
            case MUL -> {
                if (binaryVIR.target().getType() == Type.FLOAT)
                    transMulRegImmF(irs, binaryVIR.target(), reg, value.getFloat());
                else
                    transMulRegImmI(irs, binaryVIR.target(), reg, value.getInt());
            }
            case SUB -> {
                if (binaryVIR.target().getType() == Type.FLOAT)
                    transSubImmRegF(irs, binaryVIR.target(), value.getFloat(), reg);
                else
                    transSubImmRegI(irs, binaryVIR.target(), value.getInt(), reg);
            }
            default -> throw new RuntimeException();
        }
    }

    static void transBinaryRegImm(List<MIR> irs, BinaryVIR binaryVIR, VReg reg, Value value) {
        switch (binaryVIR.type()) {
            case ADD -> {
                if (binaryVIR.target().getType() == Type.FLOAT)
                    transAddRegImmF(irs, binaryVIR.target(), reg, value.getFloat());
                else
                    transAddRegImmI(irs, binaryVIR.target(), reg, value.getInt());
            }
            case DIV -> {
                if (binaryVIR.target().getType() == Type.FLOAT)
                    transDivRegImmF(irs, binaryVIR.target(), reg, value.getFloat());
                else
                    transDivRegImmI(irs, binaryVIR.target(), reg, value.getInt());
            }
            case EQ, GE, GT, LE, LT, NE -> {
                if (reg.getType() == Type.FLOAT)
                    transCmpRegImmF(irs, binaryVIR.type(), binaryVIR.target(), reg, value.getFloat());
                else
                    transCmpRegImmI(irs, binaryVIR.type(), binaryVIR.target(), reg, value.getInt());
            }
            case MOD -> transModRegImm(irs, binaryVIR.target(), reg, value.getInt());
            case MUL -> {
                if (binaryVIR.target().getType() == Type.FLOAT)
                    transMulRegImmF(irs, binaryVIR.target(), reg, value.getFloat());
                else
                    transMulRegImmI(irs, binaryVIR.target(), reg, value.getInt());
            }
            case SUB -> {
                if (binaryVIR.target().getType() == Type.FLOAT)
                    transSubRegImmF(irs, binaryVIR.target(), reg, value.getFloat());
                else
                    transSubRegImmI(irs, binaryVIR.target(), reg, value.getInt());
            }
            default -> throw new RuntimeException();
        }
    }

    static void transBinaryRegReg(List<MIR> irs, BinaryVIR binaryVIR, VReg reg1, VReg reg2) {
        switch (binaryVIR.type()) {
            case ADD -> {
                if (binaryVIR.target().getType() == Type.FLOAT)
                    transAddRegRegF(irs, binaryVIR.target(), reg1, reg2);
                else
                    transAddRegRegI(irs, binaryVIR.target(), reg1, reg2);
            }
            case DIV -> {
                if (binaryVIR.target().getType() == Type.FLOAT)
                    transDivRegRegF(irs, binaryVIR.target(), reg1, reg2);
                else
                    transDivRegRegI(irs, binaryVIR.target(), reg1, reg2);
            }
            case EQ, GE, GT, LE, LT, NE -> {
                if (reg1.getType() == Type.FLOAT)
                    transCmpRegRegF(irs, binaryVIR.type(), binaryVIR.target(), reg1, reg2);
                else
                    transCmpRegRegI(irs, binaryVIR.type(), binaryVIR.target(), reg1, reg2);
            }
            case MOD -> transModRegReg(irs, binaryVIR.target(), reg1, reg2);
            case MUL -> {
                if (binaryVIR.target().getType() == Type.FLOAT)
                    transMulRegRegF(irs, binaryVIR.target(), reg1, reg2);
                else
                    transMulRegRegI(irs, binaryVIR.target(), reg1, reg2);
            }
            case SUB -> {
                if (binaryVIR.target().getType() == Type.FLOAT)
                    transSubRegRegF(irs, binaryVIR.target(), reg1, reg2);
                else
                    transSubRegRegI(irs, binaryVIR.target(), reg1, reg2);
            }
            default -> throw new RuntimeException();
        }
    }

    private static void transCmpRegImmF(List<MIR> irs, BinaryVIR.Type type, VReg target, VReg source, float imm) {
        VReg midReg = new VReg(Type.FLOAT, 4);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transCmpRegRegF(irs, type, target, source, midReg);
    }

    private static void transCmpRegImmI(List<MIR> irs, BinaryVIR.Type type, VReg target, VReg source, int imm) {
        VReg midReg = new VReg(Type.INT, 4);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transCmpRegRegI(irs, type, target, source, midReg);
    }

    private static void transCmpRegRegF(List<MIR> irs, BinaryVIR.Type type, VReg target, VReg source1, VReg source2) {
        if (type == BinaryVIR.Type.NE) {
            irs.add(new RrrMIR(RrrMIR.Op.EQ, target, source1, source2));
            irs.add(new RriMIR(RriMIR.Op.SLTIU, target, target, 1));
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
                VReg midReg = new VReg(Type.INT, 4);
                irs.add(new RrrMIR(RrrMIR.Op.SUB, midReg, source1, source2));
                irs.add(new RriMIR(RriMIR.Op.SLTIU, target, midReg, 1));
            }
            case NE -> {
                VReg midReg1 = new VReg(Type.INT, 4);
                VReg midReg2 = new VReg(Type.INT, 4);
                irs.add(new RrrMIR(RrrMIR.Op.SUB, midReg1, source1, source2));
                irs.add(new RriMIR(RriMIR.Op.SLTIU, midReg2, midReg1, 1));
                irs.add(new RriMIR(RriMIR.Op.SLTIU, target, midReg2, 1));
            }
            case GE -> {
                VReg midReg = new VReg(Type.INT, 4);
                irs.add(new RrrMIR(RrrMIR.Op.SUB, midReg, source2, source1));
                irs.add(new RriMIR(RriMIR.Op.SLTI, target, midReg, 1));
            }
            case GT -> {
                VReg midReg = new VReg(Type.INT, 4);
                irs.add(new RrrMIR(RrrMIR.Op.SUB, midReg, source2, source1));
                irs.add(new RriMIR(RriMIR.Op.SLTI, target, midReg, 0));
            }
            case LE -> {
                VReg midReg = new VReg(Type.INT, 4);
                irs.add(new RrrMIR(RrrMIR.Op.SUB, midReg, source1, source2));
                irs.add(new RriMIR(RriMIR.Op.SLTI, target, midReg, 1));
            }
            case LT -> {
                VReg midReg = new VReg(Type.INT, 4);
                irs.add(new RrrMIR(RrrMIR.Op.SUB, midReg, source1, source2));
                irs.add(new RriMIR(RriMIR.Op.SLTI, target, midReg, 0));
            }
        }
    }

    private static void transDivImmRegF(List<MIR> irs, VReg target, float imm, VReg source) {
        VReg midReg = new VReg(Type.FLOAT, 4);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transDivRegRegF(irs, target, midReg, source);
    }

    private static void transDivImmRegI(List<MIR> irs, VReg target, int imm, VReg source) {
        VReg midReg = new VReg(Type.INT, 4);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transDivRegRegI(irs, target, midReg, source);
    }

    private static void transDivRegImmF(List<MIR> irs, VReg target, VReg source, float imm) {
        VReg midReg = new VReg(Type.FLOAT, 4);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transDivRegRegF(irs, target, source, midReg);
    }

    private static void transDivRegImmI(List<MIR> irs, VReg target, VReg source, int imm) {
        VReg midReg = new VReg(Type.INT, 4);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        irs.add(new RrrMIR(RrrMIR.Op.DIV, target, source, midReg));
    }

    private static void transDivRegRegF(List<MIR> irs, VReg target, VReg source1, VReg source2) {
        irs.add(new RrrMIR(RrrMIR.Op.DIV, target, source1, source2));
    }

    private static void transDivRegRegI(List<MIR> irs, VReg target, VReg source1, VReg source2) {
        irs.add(new RrrMIR(RrrMIR.Op.DIV, target, source1, source2));
    }

    private static void transModImmReg(List<MIR> irs, VReg target, int imm, VReg source) {
        VReg midReg = new VReg(Type.INT, 4);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transModRegReg(irs, target, midReg, source);
    }

    private static void transModRegImm(List<MIR> irs, VReg target, VReg source, int imm) {
        VReg midReg = new VReg(Type.INT, 4);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transModRegReg(irs, target, source, midReg);
    }

    private static void transModRegReg(List<MIR> irs, VReg target, VReg source1, VReg source2) {
        irs.add(new RrrMIR(RrrMIR.Op.REM, target, source1, source2));
    }

    private static void transMulRegImmF(List<MIR> irs, VReg target, VReg source, float imm) {
        VReg midReg = new VReg(Type.FLOAT, 4);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transMulRegRegF(irs, target, source, midReg);
    }

    private static void transMulRegImmI(List<MIR> irs, VReg target, VReg source, int imm) {
        VReg midReg = new VReg(Type.INT, 4);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transMulRegRegI(irs, target, source, midReg);
    }

    private static void transMulRegRegF(List<MIR> irs, VReg target, VReg source1, VReg source2) {
        irs.add(new RrrMIR(RrrMIR.Op.MUL, target, source1, source2));
    }

    private static void transMulRegRegI(List<MIR> irs, VReg target, VReg source1, VReg source2) {
        irs.add(new RrrMIR(RrrMIR.Op.MUL, target, source1, source2));
    }

    private static void transSubImmRegF(List<MIR> irs, VReg target, float imm, VReg source) {
        VReg midReg = new VReg(Type.FLOAT, 4);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transSubRegRegF(irs, target, midReg, source);
    }

    private static void transSubImmRegI(List<MIR> irs, VReg target, int imm, VReg source) {
        VReg midReg = new VReg(Type.INT, 4);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transSubRegRegI(irs, target, midReg, source);
    }

    private static void transSubRegImmF(List<MIR> irs, VReg target, VReg source, float imm) {
        VReg midReg = new VReg(Type.FLOAT, 4);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transSubRegRegF(irs, target, source, midReg);
    }

    private static void transSubRegImmI(List<MIR> irs, VReg target, VReg source, int imm) {
        if (-imm >= -2048 && -imm < 2048) {
            irs.add(new RriMIR(RriMIR.Op.ADDI, target, source, -imm));
            return;
        }
        VReg midReg = new VReg(Type.INT, 4);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transSubRegRegI(irs, target, source, midReg);
    }

    private static void transSubRegRegF(List<MIR> irs, VReg target, VReg source1, VReg source2) {
        irs.add(new RrrMIR(RrrMIR.Op.SUB, target, source1, source2));
    }

    private static void transSubRegRegI(List<MIR> irs, VReg target, VReg source1, VReg source2) {
        irs.add(new RrrMIR(RrrMIR.Op.SUB, target, source1, source2));
    }
}
