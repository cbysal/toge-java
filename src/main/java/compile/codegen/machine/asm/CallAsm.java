package compile.codegen.machine.asm;

import compile.codegen.machine.reg.MReg;
import compile.codegen.machine.reg.VReg;

import java.util.List;
import java.util.Map;

public class CallAsm implements Asm {
    private final String name;

    public CallAsm(String name) {
        this.name = name;
    }

    @Override
    public List<VReg> getVRegs() {
        return List.of();
    }

    @Override
    public void replaceVRegs(Map<VReg, MReg> vRegToMReg) {
    }

    @Override
    public List<Asm> spill(Map<VReg, Integer> vRegToSpill) {
        return List.of(this);
    }

    @Override
    public String toString() {
        return String.format("call %s", name);
    }
}
