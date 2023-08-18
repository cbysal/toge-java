package compile.codegen.virgen.vir;

import compile.codegen.virgen.VReg;

import java.util.List;

public class MovVIR extends VIR {
    public final VReg target;
    public final VReg source;

    public MovVIR(VReg target, VReg source) {
        this.target = target;
        this.source = source;
    }

    @Override
    public List<VReg> getRead() {
        return List.of(source);
    }

    @Override
    public VReg getWrite() {
        return target;
    }

    @Override
    public String toString() {
        return "MOV     " + target + ", " + source;
    }
}
