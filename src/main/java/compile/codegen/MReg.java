package compile.codegen;

import compile.vir.type.BasicType;
import compile.vir.type.Type;

import java.util.List;

public class MReg extends Reg {
    public static final MReg ZERO = new MReg(BasicType.I32, "zero");
    public static final MReg RA = new MReg(BasicType.I32, "ra");
    public static final MReg SP = new MReg(BasicType.I32, "sp");
    public static final MReg GP = new MReg(BasicType.I32, "gp");
    public static final MReg TP = new MReg(BasicType.I32, "tp");
    public static final MReg T0 = new MReg(BasicType.I32, "t0");
    public static final MReg T1 = new MReg(BasicType.I32, "t1");
    public static final MReg T2 = new MReg(BasicType.I32, "t2");
    public static final MReg S0 = new MReg(BasicType.I32, "s0");
    public static final MReg S1 = new MReg(BasicType.I32, "s1");
    public static final MReg A0 = new MReg(BasicType.I32, "a0");
    public static final MReg A1 = new MReg(BasicType.I32, "a1");
    public static final MReg A2 = new MReg(BasicType.I32, "a2");
    public static final MReg A3 = new MReg(BasicType.I32, "a3");
    public static final MReg A4 = new MReg(BasicType.I32, "a4");
    public static final MReg A5 = new MReg(BasicType.I32, "a5");
    public static final MReg A6 = new MReg(BasicType.I32, "a6");
    public static final MReg A7 = new MReg(BasicType.I32, "a7");
    public static final MReg S2 = new MReg(BasicType.I32, "s2");
    public static final MReg S3 = new MReg(BasicType.I32, "s3");
    public static final MReg S4 = new MReg(BasicType.I32, "s4");
    public static final MReg S5 = new MReg(BasicType.I32, "s5");
    public static final MReg S6 = new MReg(BasicType.I32, "s6");
    public static final MReg S7 = new MReg(BasicType.I32, "s7");
    public static final MReg S8 = new MReg(BasicType.I32, "s8");
    public static final MReg S9 = new MReg(BasicType.I32, "s9");
    public static final MReg S10 = new MReg(BasicType.I32, "s10");
    public static final MReg S11 = new MReg(BasicType.I32, "s11");
    public static final MReg T3 = new MReg(BasicType.I32, "t3");
    public static final MReg T4 = new MReg(BasicType.I32, "t4");
    public static final MReg T5 = new MReg(BasicType.I32, "t5");
    public static final MReg T6 = new MReg(BasicType.I32, "t6");
    public static final MReg FA0 = new MReg(BasicType.FLOAT, "fa0");
    public static final MReg FA1 = new MReg(BasicType.FLOAT, "fa1");
    public static final MReg FA2 = new MReg(BasicType.FLOAT, "fa2");
    public static final MReg FA3 = new MReg(BasicType.FLOAT, "fa3");
    public static final MReg FA4 = new MReg(BasicType.FLOAT, "fa4");
    public static final MReg FA5 = new MReg(BasicType.FLOAT, "fa5");
    public static final MReg FA6 = new MReg(BasicType.FLOAT, "fa6");
    public static final MReg FA7 = new MReg(BasicType.FLOAT, "fa7");
    public static final MReg FS0 = new MReg(BasicType.FLOAT, "fs0");
    public static final MReg FS1 = new MReg(BasicType.FLOAT, "fs1");
    public static final MReg FS2 = new MReg(BasicType.FLOAT, "fs2");
    public static final MReg FS3 = new MReg(BasicType.FLOAT, "fs3");
    public static final MReg FS4 = new MReg(BasicType.FLOAT, "fs4");
    public static final MReg FS5 = new MReg(BasicType.FLOAT, "fs5");
    public static final MReg FS6 = new MReg(BasicType.FLOAT, "fs6");
    public static final MReg FS7 = new MReg(BasicType.FLOAT, "fs7");
    public static final MReg FS8 = new MReg(BasicType.FLOAT, "fs8");
    public static final MReg FS9 = new MReg(BasicType.FLOAT, "fs9");
    public static final MReg FS10 = new MReg(BasicType.FLOAT, "fs10");
    public static final MReg FS11 = new MReg(BasicType.FLOAT, "fs11");
    public static final MReg FT0 = new MReg(BasicType.FLOAT, "ft0");
    public static final MReg FT1 = new MReg(BasicType.FLOAT, "ft1");
    public static final MReg FT2 = new MReg(BasicType.FLOAT, "ft2");
    public static final MReg FT3 = new MReg(BasicType.FLOAT, "ft3");
    public static final MReg FT4 = new MReg(BasicType.FLOAT, "ft4");
    public static final MReg FT5 = new MReg(BasicType.FLOAT, "ft5");
    public static final MReg FT6 = new MReg(BasicType.FLOAT, "ft6");
    public static final MReg FT7 = new MReg(BasicType.FLOAT, "ft7");
    public static final MReg FT8 = new MReg(BasicType.FLOAT, "ft8");
    public static final MReg FT9 = new MReg(BasicType.FLOAT, "ft9");
    public static final MReg FT10 = new MReg(BasicType.FLOAT, "ft10");
    public static final MReg FT11 = new MReg(BasicType.FLOAT, "ft11");
    public static final List<MReg> I_REGS = List.of(MReg.A0, MReg.A1, MReg.A2, MReg.A3, MReg.A4, MReg.A5, MReg.A6,
            MReg.A7, MReg.S0, MReg.S1, MReg.S2, MReg.S3, MReg.S4, MReg.S5, MReg.S6, MReg.S7, MReg.S8, MReg.S9,
            MReg.S10, MReg.S11);
    public static final List<MReg> F_REGS = List.of(MReg.FA0, MReg.FA1, MReg.FA2, MReg.FA3, MReg.FA4, MReg.FA5,
            MReg.FA6, MReg.FA7, MReg.FS0, MReg.FS1, MReg.FS2, MReg.FS3, MReg.FS4, MReg.FS5, MReg.FS6, MReg.FS7,
            MReg.FS8, MReg.FS9, MReg.FS10, MReg.FS11);
    public static final List<MReg> I_CALLER_REGS = List.of(MReg.A0, MReg.A1, MReg.A2, MReg.A3, MReg.A4, MReg.A5,
            MReg.A6, MReg.A7);
    public static final List<MReg> F_CALLER_REGS = List.of(MReg.FA0, MReg.FA1, MReg.FA2, MReg.FA3, MReg.FA4, MReg.FA5
            , MReg.FA6, MReg.FA7);
    public static final List<MReg> I_CALLEE_REGS = List.of(MReg.S0, MReg.S1, MReg.S2, MReg.S3, MReg.S4, MReg.S5,
            MReg.S6, MReg.S7, MReg.S8, MReg.S9, MReg.S10, MReg.S11);
    public static final List<MReg> F_CALLEE_REGS = List.of(MReg.FS0, MReg.FS1, MReg.FS2, MReg.FS3, MReg.FS4, MReg.FS5
            , MReg.FS6, MReg.FS7, MReg.FS8, MReg.FS9, MReg.FS10, MReg.FS11);
    private final String name;

    private MReg(Type type, String name) {
        super(type, 8);
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
