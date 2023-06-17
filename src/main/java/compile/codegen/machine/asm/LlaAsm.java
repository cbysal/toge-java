package compile.codegen.machine.asm;

import compile.codegen.machine.DataItem;
import compile.codegen.machine.reg.MReg;
import compile.codegen.machine.reg.Reg;
import compile.codegen.machine.reg.VReg;

import java.util.List;
import java.util.Map;

public record LlaAsm(Reg dest, DataItem src) implements Asm {
    @Override
    public List<VReg> getVRegs() {
        if (dest instanceof VReg vReg) {
            return List.of(vReg);
        }
        return List.of();
    }

    @Override
    public Asm replaceVRegs(Map<VReg, MReg> vRegToMReg) {
        if (dest instanceof VReg vDest && vRegToMReg.containsKey(vDest)) {
            return new LlaAsm(vRegToMReg.get(vDest), src);
        }
        return this;
    }

    @Override
    public List<Asm> spill(Map<VReg, Integer> vRegToSpill) {
        if (dest instanceof VReg && vRegToSpill.containsKey(dest)) {
            VReg newDest = new VReg(dest.isFloat());
            int spill = vRegToSpill.get(dest);
            return List.of(new LlaAsm(newDest, src), new VStoreSpillAsm(newDest, spill));
        }
        return List.of(this);
    }

    @Override
    public String toString() {
        return String.format("lla %s,%s", dest, src.getName());
    }
}
