package compile.llvm.ir;

import compile.llvm.type.Type;
import compile.llvm.value.User;

public abstract class Instruction extends User {
    private static int counter = 0;
    protected final int id;

    protected Instruction(Type type) {
        super(type);
        this.id = counter++;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Instruction vir = (Instruction) o;
        return id == vir.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public final String getName() {
        return "%v" + id;
    }
}
