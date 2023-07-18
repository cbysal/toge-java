package compile.codegen.virgen;

import compile.codegen.Reg;
import compile.codegen.virgen.vir.VIRItem;
import compile.symbol.Type;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class VReg extends Reg implements VIRItem {
    private static final AtomicInteger counter = new AtomicInteger(0);
    private final int id;

    public VReg(Type type, int size) {
        super(type, size);
        this.id = counter.getAndIncrement();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        VReg vReg = (VReg) o;
        return id == vReg.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }


    @Override
    public String toString() {
        return (type == Type.FLOAT ? "$f" : "$i") + id;
    }
}
