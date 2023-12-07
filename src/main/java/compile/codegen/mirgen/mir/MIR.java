package compile.codegen.mirgen.mir;

import compile.codegen.Reg;
import compile.codegen.MReg;
import compile.codegen.VReg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class MIR {
    private static int counter = 0;
    private final int id;

    public MIR() {
        this.id = counter++;
    }

    public List<Reg> getRead() {
        return List.of();
    }

    public List<Reg> getRegs() {
        List<Reg> regs = new ArrayList<>();
        regs.addAll(getRead());
        regs.addAll(getWrite());
        return regs;
    }

    public List<Reg> getWrite() {
        return List.of();
    }

    public MIR replaceReg(Map<VReg, MReg> replaceMap) {
        return this;
    }

    public List<MIR> spill(Reg reg, int offset) {
        return List.of(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MIR mir = (MIR) o;
        return id == mir.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
