package compile.codegen.mirgen.mir;

import compile.codegen.Reg;
import compile.codegen.mirgen.MReg;
import compile.codegen.virgen.VReg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface MIR {
    default List<Reg> getRead() {
        return List.of();
    }

    default List<Reg> getRegs() {
        List<Reg> regs = new ArrayList<>();
        regs.addAll(getRead());
        regs.addAll(getWrite());
        return regs;
    }

    default List<Reg> getWrite() {
        return List.of();
    }

    default MIR replaceReg(Map<VReg, MReg> replaceMap) {
        return this;
    }

    default List<MIR> spill(Reg reg, int offset) {
        return List.of(this);
    }
}
