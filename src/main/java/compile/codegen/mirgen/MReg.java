package compile.codegen.mirgen;

import compile.codegen.Reg;
import compile.symbol.Type;

import java.util.List;

public class MReg extends Reg {
    public static final MReg ZERO = new MReg(Type.INT, "zero");
    public static final MReg RA = new MReg(Type.INT, "ra");
    public static final MReg SP = new MReg(Type.INT, "sp");
    public static final MReg GP = new MReg(Type.INT, "gp");
    public static final MReg TP = new MReg(Type.INT, "tp");
    public static final MReg T0 = new MReg(Type.INT, "t0");
    public static final MReg T1 = new MReg(Type.INT, "t1");
    public static final MReg T2 = new MReg(Type.INT, "t2");
    public static final MReg S0 = new MReg(Type.INT, "s0");
    public static final MReg S1 = new MReg(Type.INT, "s1");
    public static final MReg A0 = new MReg(Type.INT, "a0");
    public static final MReg A1 = new MReg(Type.INT, "a1");
    public static final MReg A2 = new MReg(Type.INT, "a2");
    public static final MReg A3 = new MReg(Type.INT, "a3");
    public static final MReg A4 = new MReg(Type.INT, "a4");
    public static final MReg A5 = new MReg(Type.INT, "a5");
    public static final MReg A6 = new MReg(Type.INT, "a6");
    public static final MReg A7 = new MReg(Type.INT, "a7");
    public static final MReg S2 = new MReg(Type.INT, "s2");
    public static final MReg S3 = new MReg(Type.INT, "s3");
    public static final MReg S4 = new MReg(Type.INT, "s4");
    public static final MReg S5 = new MReg(Type.INT, "s5");
    public static final MReg S6 = new MReg(Type.INT, "s6");
    public static final MReg S7 = new MReg(Type.INT, "s7");
    public static final MReg S8 = new MReg(Type.INT, "s8");
    public static final MReg S9 = new MReg(Type.INT, "s9");
    public static final MReg S10 = new MReg(Type.INT, "s10");
    public static final MReg S11 = new MReg(Type.INT, "s11");
    public static final MReg T3 = new MReg(Type.INT, "t3");
    public static final MReg T4 = new MReg(Type.INT, "t4");
    public static final MReg T5 = new MReg(Type.INT, "t5");
    public static final MReg T6 = new MReg(Type.INT, "t6");
    public static final MReg FA0 = new MReg(Type.FLOAT, "fa0");
    public static final MReg FA1 = new MReg(Type.FLOAT, "fa1");
    public static final MReg FA2 = new MReg(Type.FLOAT, "fa2");
    public static final MReg FA3 = new MReg(Type.FLOAT, "fa3");
    public static final MReg FA4 = new MReg(Type.FLOAT, "fa4");
    public static final MReg FA5 = new MReg(Type.FLOAT, "fa5");
    public static final MReg FA6 = new MReg(Type.FLOAT, "fa6");
    public static final MReg FA7 = new MReg(Type.FLOAT, "fa7");
    public static final MReg FS0 = new MReg(Type.FLOAT, "fs0");
    public static final MReg FS1 = new MReg(Type.FLOAT, "fs1");
    public static final MReg FS2 = new MReg(Type.FLOAT, "fs2");
    public static final MReg FS3 = new MReg(Type.FLOAT, "fs3");
    public static final MReg FS4 = new MReg(Type.FLOAT, "fs4");
    public static final MReg FS5 = new MReg(Type.FLOAT, "fs5");
    public static final MReg FS6 = new MReg(Type.FLOAT, "fs6");
    public static final MReg FS7 = new MReg(Type.FLOAT, "fs7");
    public static final MReg FS8 = new MReg(Type.FLOAT, "fs8");
    public static final MReg FS9 = new MReg(Type.FLOAT, "fs9");
    public static final MReg FS10 = new MReg(Type.FLOAT, "fs10");
    public static final MReg FS11 = new MReg(Type.FLOAT, "fs11");
    public static final MReg FT0 = new MReg(Type.FLOAT, "ft0");
    public static final MReg FT1 = new MReg(Type.FLOAT, "ft1");
    public static final MReg FT2 = new MReg(Type.FLOAT, "ft2");
    public static final MReg FT3 = new MReg(Type.FLOAT, "ft3");
    public static final MReg FT4 = new MReg(Type.FLOAT, "ft4");
    public static final MReg FT5 = new MReg(Type.FLOAT, "ft5");
    public static final MReg FT6 = new MReg(Type.FLOAT, "ft6");
    public static final MReg FT7 = new MReg(Type.FLOAT, "ft7");
    public static final MReg FT8 = new MReg(Type.FLOAT, "ft8");
    public static final MReg FT9 = new MReg(Type.FLOAT, "ft9");
    public static final MReg FT10 = new MReg(Type.FLOAT, "ft10");
    public static final MReg FT11 = new MReg(Type.FLOAT, "ft11");
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
