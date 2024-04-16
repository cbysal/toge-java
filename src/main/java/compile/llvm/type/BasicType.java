package compile.llvm.type;

public enum BasicType implements Type {
    I1, I32, FLOAT, VOID;

    @Override
    public int getSize() {
        switch (this) {
            case I1:
                return 1;
            case I32:
            case FLOAT:
                return 32;
            case VOID:
                return 0;
            default:
                throw new IllegalArgumentException();
        }
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
