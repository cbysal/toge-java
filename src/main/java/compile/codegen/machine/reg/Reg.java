package compile.codegen.machine.reg;

public abstract class Reg {
    protected final boolean isFloat;

    protected Reg(boolean isFloat) {
        this.isFloat = isFloat;
    }

    public boolean isFloat() {
        return isFloat;
    }
}
