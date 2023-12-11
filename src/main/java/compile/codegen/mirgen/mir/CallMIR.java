package compile.codegen.mirgen.mir;

import compile.codegen.Reg;
import compile.codegen.MReg;
import compile.vir.type.BasicType;
import compile.symbol.FuncSymbol;
import compile.vir.Argument;

import java.util.ArrayList;
import java.util.List;

public class CallMIR extends MIR {
    public final FuncSymbol func;

    public CallMIR(FuncSymbol func) {
        this.func = func;
    }

    @Override
    public List<Reg> getRead() {
        List<Reg> regs = new ArrayList<>();
        int iSize = 0, fSize = 0;
        for (Argument arg : func.getArgs()) {
            if (arg.getType() == BasicType.FLOAT) {
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
