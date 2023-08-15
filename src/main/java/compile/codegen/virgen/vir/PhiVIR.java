package compile.codegen.virgen.vir;

import compile.codegen.virgen.VReg;

import java.util.List;
import java.util.Set;

public record PhiVIR(VReg target, Set<VReg> sources) implements VIR {
    @Override
    public List<VReg> getRead() {
        return List.copyOf(sources);
    }

    @Override
    public VReg getWrite() {
        return target;
    }

    @Override
    public String toString() {
        return "PHI     " + target + " <- " + sources;
    }
}
