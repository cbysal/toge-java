package compile.codegen.machine.asm;

import compile.codegen.machine.Block;
import compile.codegen.machine.reg.MReg;
import compile.codegen.machine.reg.VReg;

import java.util.List;
import java.util.Map;

public record JAsm(Block dest) implements Asm {
    @Override
    public List<VReg> getVRegs() {
        return List.of();
    }

    @Override
    public Asm replaceVRegs(Map<VReg, MReg> vRegToMReg) {
        return this;
    }

    @Override
    public List<Asm> spill(Map<VReg, Integer> vRegToSpill) {
        return List.of(this);
    }

    @Override
    public String toString() {
        return String.format("j %s", dest.getTag());
    }
}
