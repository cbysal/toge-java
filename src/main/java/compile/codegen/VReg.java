package compile.codegen;

import compile.llvm.type.BasicType;
import compile.llvm.type.Type;

public class VReg extends Reg {
    private static int counter = 0;
    private final int id;

    public VReg(Type type) {
        super(type);
        this.id = counter++;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        String invalidType;
        if (type.equals(BasicType.I32)) {
            invalidType = "$i";
        } else if (type.equals(BasicType.FLOAT)) {
            invalidType = "$f";
        } else {
            throw new RuntimeException("Invalid type");
        }
        return invalidType + id;
    }
}
