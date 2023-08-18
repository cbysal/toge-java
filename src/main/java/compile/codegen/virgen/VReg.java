package compile.codegen.virgen;

import compile.codegen.Reg;
import compile.codegen.virgen.vir.VIRItem;
import compile.symbol.Type;

public class VReg extends Reg implements VIRItem {
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
        return (type == Type.FLOAT ? "$f" : "$i") + id;
    }
}
