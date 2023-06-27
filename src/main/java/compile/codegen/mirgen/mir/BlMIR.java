package compile.codegen.mirgen.mir;

import compile.codegen.Reg;
import compile.codegen.mirgen.MReg;
import compile.symbol.FuncSymbol;
import compile.symbol.ParamSymbol;
import compile.symbol.Type;

import java.util.ArrayList;
import java.util.List;

public class BlMIR implements MIR {
    private final FuncSymbol func;

    public BlMIR(FuncSymbol func) {
        this.func = func;
    }

    public FuncSymbol getFunc() {
        return func;
    }

    @Override
    public List<Reg> getRead() {
        List<Reg> regs = new ArrayList<>();
        int iSize = 0, fSize = 0;
        for (ParamSymbol param : func.getParams()) {
            if (param.isSingle() && param.getType() == Type.FLOAT) {
                if (fSize < MReg.F_CALLER_REGS.size())
                    regs.add(MReg.F_CALLER_REGS.get(fSize));
                fSize++;
            } else {
                if (iSize < MReg.I_CALLER_REGS.size())
                    regs.add(MReg.I_CALLER_REGS.get(iSize));
                iSize++;
            }
        }
        return regs;
    }

    @Override
    public List<Reg> getRegs() {
        List<Reg> regs = new ArrayList<>();
        regs.addAll(MReg.I_CALLER_REGS);
        regs.addAll(MReg.F_CALLER_REGS);
        return regs;
    }

    @Override
    public List<Reg> getWrite() {
        List<Reg> regs = new ArrayList<>();
        regs.addAll(MReg.I_CALLER_REGS);
        regs.addAll(MReg.F_CALLER_REGS);
        return regs;
    }

    @Override
    public String toString() {
        return "call\t" + func.getName();
    }
}
