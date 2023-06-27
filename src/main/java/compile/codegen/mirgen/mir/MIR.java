package compile.codegen.mirgen.mir;

import compile.codegen.Reg;
import compile.codegen.mirgen.MReg;
import compile.codegen.virgen.VReg;

import java.util.List;
import java.util.Map;

public interface MIR {
    default List<Reg> getRead() {
        return List.of();
    }

    default List<Reg> getRegs() {
        return List.of();
    }

    default List<Reg> getWrite() {
        return List.of();
    }

    default void replaceReg(Map<VReg, MReg> replaceMap) {
    }

    default List<MIR> spill(Reg reg, int offset) {
        return List.of(this);
    }
}
