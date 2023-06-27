package compile.codegen.virgen.vir;

import compile.codegen.virgen.VReg;

import java.util.List;

public class MovVIR implements VIR {
    private final VReg target, source;

    public MovVIR(VReg target, VReg source) {
        this.target = target;
        this.source = source;
    }

    @Override
    public List<VReg> getRead() {
        return List.of(source);
    }

    public VReg getSource() {
        return source;
    }

    public VReg getTarget() {
        return target;
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
