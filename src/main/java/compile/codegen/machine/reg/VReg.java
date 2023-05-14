package compile.codegen.machine.reg;

public class VReg extends Reg {
    private static int autoTag;
    private final int id;

    public VReg(boolean isFloat) {
        super(isFloat);
        this.id = autoTag++;
    }

    @Override
    public String toString() {
        return String.format("%%v%d", id);
    }
}
