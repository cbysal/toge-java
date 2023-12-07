package compile.codegen;

import compile.vir.type.BasicType;
import compile.vir.type.Type;

public class VReg extends Reg {
    private static int counter = 0;
    private final int id;

    public VReg(Type type, int size) {
        super(type, size);
        this.id = counter++;
    }

    @Override
    public int hashCode() {
        return id;
    }


    @Override
    public String toString() {
        return (type == BasicType.FLOAT ? "$f" : "$i") + id;
    }
}
