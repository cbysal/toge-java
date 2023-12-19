package compile.codegen.mirgen.mir;

import compile.codegen.MReg;
import compile.codegen.Reg;
import compile.llvm.Argument;
import compile.llvm.Function;
import compile.llvm.type.BasicType;

import java.util.ArrayList;
import java.util.List;

public class CallMIR extends MIR {
    public final Function func;

    public CallMIR(Function func) {
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
        return "call\t" + func.getRawName();
    }
}
