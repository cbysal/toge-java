package compile.codegen.machine.asm.virtual;

import compile.codegen.machine.asm.Asm;
import compile.codegen.machine.reg.MReg;
import compile.codegen.machine.reg.Reg;
import compile.codegen.machine.reg.VReg;

import java.util.List;
import java.util.Map;

public record VRetRegAsm(Reg reg) implements Asm {
    @Override
    public List<VReg> getVRegs() {
        if (reg instanceof VReg vReg) {
            return List.of(vReg);
        }
        return List.of();
    }

    @Override
    public Asm replaceVRegs(Map<VReg, MReg> vRegToMReg) {
        if (reg instanceof VReg vReg && vRegToMReg.containsKey(vReg)) {
            return new VRetRegAsm(vRegToMReg.get(vReg));
        }
        return this;
    }

    @Override
    public List<Asm> spill(Map<VReg, Integer> vRegToSpill) {
        return List.of(this);
    }
}
