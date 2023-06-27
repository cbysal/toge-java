package compile.codegen.virgen.vir;

import compile.codegen.virgen.VReg;

import java.util.List;

public interface VIR {
    List<VReg> getRead();

    VReg getWrite();
}
