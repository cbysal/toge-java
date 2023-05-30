package compile.codegen.machine.asm;

import compile.codegen.machine.reg.MReg;
import compile.codegen.machine.reg.VReg;

import java.util.List;
import java.util.Map;

public interface Asm {
    List<VReg> getVRegs();

    Asm replaceVRegs(Map<VReg, MReg> vRegToMReg);

    List<Asm> spill(Map<VReg, Integer> vRegToSpill);
}
