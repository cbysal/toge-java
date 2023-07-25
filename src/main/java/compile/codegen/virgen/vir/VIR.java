package compile.codegen.virgen.vir;

import compile.codegen.virgen.VReg;

import java.util.List;

public interface VIR {
    default List<VReg> getRead() {
        return List.of();
    }

    default VReg getWrite() {
        return null;
    }
}
