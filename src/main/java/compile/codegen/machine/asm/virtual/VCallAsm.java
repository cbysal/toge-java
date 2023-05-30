package compile.codegen.machine.asm.virtual;

import compile.codegen.machine.asm.Asm;
import compile.codegen.machine.reg.MReg;
import compile.codegen.machine.reg.Reg;
import compile.codegen.machine.reg.VReg;

import java.util.List;
import java.util.Map;

public record VCallAsm(String name, List<Reg> params) implements Asm {
    @Override
    public List<VReg> getVRegs() {
        return params.stream().filter(param -> param instanceof VReg).map(param -> (VReg) param).toList();
    }

    @Override
    public Asm replaceVRegs(Map<VReg, MReg> vRegToMReg) {
        List<Reg> newParams = params.stream().map(reg -> {
            if (reg instanceof VReg vReg && vRegToMReg.containsKey(vReg)) {
                return vRegToMReg.get(vReg);
            }
            return reg;
        }).toList();
        return new VCallAsm(name, newParams);
    }

    @Override
    public List<Asm> spill(Map<VReg, Integer> vRegToSpill) {
        return List.of(this);
    }
}
