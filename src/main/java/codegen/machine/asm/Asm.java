package codegen.machine.asm;

import codegen.machine.reg.MReg;
import codegen.machine.reg.VReg;

import java.util.List;
import java.util.Map;

public interface Asm {
    List<VReg> getVRegs();

    void replaceVRegs(Map<VReg, MReg> vRegToMReg);

    List<Asm> spill(Map<VReg, Integer> vRegToSpill);
}
