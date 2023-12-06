package compile.vir.ir;

import compile.vir.VReg;

import java.util.List;

public abstract class VIR {
    private static int counter = 0;
    private final int id;

    protected VIR() {
        this.id = counter++;
    }

    abstract public VIR copy();

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        VIR vir = (VIR) o;
        return id == vir.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    public List<VReg> getRead() {
        return List.of();
    }

    public VReg getWrite() {
        return null;
    }
}
