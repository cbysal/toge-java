package codegen.machine.reg;

import java.util.List;

public class MReg extends Reg {
    private final String name;
    public static final MReg ZERO = new MReg(false, "zero");
    public static final MReg RA = new MReg(false, "ra");
    public static final MReg SP = new MReg(false, "sp");
    public static final MReg GP = new MReg(false, "gp");
    public static final MReg TP = new MReg(false, "tp");
    public static final MReg T0 = new MReg(false, "t0");
    public static final MReg T1 = new MReg(false, "t1");
    public static final MReg T2 = new MReg(false, "t2");
    public static final MReg S0 = new MReg(false, "s0");
    public static final MReg S1 = new MReg(false, "s1");
    public static final MReg A0 = new MReg(false, "a0");
    public static final MReg A1 = new MReg(false, "a1");
    public static final MReg A2 = new MReg(false, "a2");
    public static final MReg A3 = new MReg(false, "a3");
    public static final MReg A4 = new MReg(false, "a4");
    public static final MReg A5 = new MReg(false, "a5");
    public static final MReg A6 = new MReg(false, "a6");
    public static final MReg A7 = new MReg(false, "a7");
    public static final MReg S2 = new MReg(false, "s2");
    public static final MReg S3 = new MReg(false, "s3");
    public static final MReg S4 = new MReg(false, "s4");
    public static final MReg S5 = new MReg(false, "s5");
    public static final MReg S6 = new MReg(false, "s6");
    public static final MReg S7 = new MReg(false, "s7");
    public static final MReg S8 = new MReg(false, "s8");
    public static final MReg S9 = new MReg(false, "s9");
    public static final MReg S10 = new MReg(false, "s10");
    public static final MReg S11 = new MReg(false, "s11");
    public static final MReg T3 = new MReg(false, "t3");
    public static final MReg T4 = new MReg(false, "t4");
    public static final MReg T5 = new MReg(false, "t5");
    public static final MReg T6 = new MReg(false, "t6");
    public static final MReg FA0 = new MReg(true, "fa0");
    public static final MReg FA1 = new MReg(true, "fa1");
    public static final MReg FA2 = new MReg(true, "fa2");
    public static final MReg FA3 = new MReg(true, "fa3");
    public static final MReg FA4 = new MReg(true, "fa4");
    public static final MReg FA5 = new MReg(true, "fa5");
    public static final MReg FA6 = new MReg(true, "fa6");
    public static final MReg FA7 = new MReg(true, "fa7");
    public static final MReg FS0 = new MReg(true, "fs0");
    public static final MReg FS1 = new MReg(true, "fs1");
    public static final MReg FS2 = new MReg(true, "fs2");
    public static final MReg FS3 = new MReg(true, "fs3");
    public static final MReg FS4 = new MReg(true, "fs4");
    public static final MReg FS5 = new MReg(true, "fs5");
    public static final MReg FS6 = new MReg(true, "fs6");
    public static final MReg FS7 = new MReg(true, "fs7");
    public static final MReg FS8 = new MReg(true, "fs8");
    public static final MReg FS9 = new MReg(true, "fs9");
    public static final MReg FS10 = new MReg(true, "fs10");
    public static final MReg FS11 = new MReg(true, "fs11");
    public static final MReg FT0 = new MReg(true, "ft0");
    public static final MReg FT1 = new MReg(true, "ft1");
    public static final MReg FT2 = new MReg(true, "ft2");
    public static final MReg FT3 = new MReg(true, "ft3");
    public static final MReg FT4 = new MReg(true, "ft4");
    public static final MReg FT5 = new MReg(true, "ft5");
    public static final MReg FT6 = new MReg(true, "ft6");
    public static final MReg FT7 = new MReg(true, "ft7");
    public static final MReg FT8 = new MReg(true, "ft8");
    public static final MReg FT9 = new MReg(true, "ft9");
    public static final MReg FT10 = new MReg(true, "ft10");
    public static final MReg FT11 = new MReg(true, "ft11");
    public static final List<MReg> CALLER_REGS = List.of(A0, A1, A2, A3, A4, A5, A6, A7);
    public static final List<MReg> CALLEE_REGS = List.of(S0, S1, S2, S3, S4, S5, S6, S7, S8, S9, S10, S11);
    public static final List<MReg> F_CALLER_REGS = List.of(FA0, FA1, FA2, FA3, FA4, FA5, FA6, FA7);
    public static final List<MReg> F_CALLEE_REGS = List.of(FS0, FS1, FS2, FS3, FS4, FS5, FS6, FS7, FS8, FS9, FS10,
            FS11);

    private MReg(boolean isFloat, String name) {
        super(isFloat);
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
