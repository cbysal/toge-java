package compile.codegen.machine.asm;

import compile.codegen.machine.reg.MReg;
import compile.codegen.machine.reg.Reg;
import compile.codegen.machine.reg.VReg;

import java.util.List;
import java.util.Map;

public record VRetRegAsm(Reg ret) implements Asm {
    @Override
    public List<VReg> getVRegs() {
        if (ret instanceof VReg vRet) {
            return List.of(vRet);
        }
        return List.of();
    }

    @Override
    public Asm replaceVRegs(Map<VReg, MReg> vRegToMReg) {
        if (ret instanceof VReg && vRegToMReg.containsKey(ret)) {
            return new VRetRegAsm(vRegToMReg.get(ret));
        }
        return this;
    }

    @Override
    public List<Asm> spill(Map<VReg, Integer> vRegToSpill) {
        if (ret instanceof VReg && vRegToSpill.containsKey(ret)) {
            VReg newRet = new VReg(ret.isFloat());
            int spill = vRegToSpill.get(ret);
            return List.of(new VLoadSpillAsm(newRet, spill), new VRetRegAsm(newRet));
        }
        return List.of(this);
    }
}
