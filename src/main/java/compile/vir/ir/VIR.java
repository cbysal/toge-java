package compile.vir.ir;

import compile.vir.type.Type;
import compile.vir.value.User;

public abstract class VIR extends User {
    private static int counter = 0;
    protected final int id;

    protected VIR(Type type) {
        super(type);
        this.id = counter++;
    }

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

    public final String getTag() {
        return "%" + id;
    }
}
