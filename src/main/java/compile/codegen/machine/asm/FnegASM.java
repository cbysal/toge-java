package compile.codegen.machine.asm;

import compile.codegen.machine.reg.MReg;
import compile.codegen.machine.reg.Reg;
import compile.codegen.machine.reg.VReg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FnegASM implements Asm {
    private Reg dest, src;

    public FnegASM(Reg dest, Reg src) {
        this.dest = dest;
        this.src = src;
    }

    @Override
    public List<VReg> getVRegs() {
        List<VReg> vRegs = new ArrayList<>();
        if (dest instanceof VReg vReg) {
            vRegs.add(vReg);
        }
        if (src instanceof VReg vReg) {
            vRegs.add(vReg);
        }
        return vRegs;
    }

    @Override
    public void replaceVRegs(Map<VReg, MReg> vRegToMReg) {
        if (dest instanceof VReg vReg && vRegToMReg.containsKey(vReg)) {
            dest = vRegToMReg.get(vReg);
        }
        if (src instanceof VReg vReg && vRegToMReg.containsKey(vReg)) {
            src = vRegToMReg.get(vReg);
        }
    }

    @Override
    public List<Asm> spill(Map<VReg, Integer> vRegToSpill) {
        return List.of(this);
    }

    @Override
    public String toString() {
        return String.format("fneg.s %s,%s", dest, src);
    }
}
