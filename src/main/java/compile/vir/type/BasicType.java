package compile.vir.type;

public enum BasicType implements Type {
    I1, I32, FLOAT, VOID;

    @Override
    public int getSize() {
        return switch (this) {
            case I1 -> 1;
            case I32, FLOAT -> 32;
            case VOID -> 0;
        };
    }

    @Override
    public Type baseType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
