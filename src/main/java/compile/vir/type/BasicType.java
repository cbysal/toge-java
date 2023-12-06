package compile.vir.type;

public enum BasicType implements Type {
    I32, FLOAT, VOID;

    @Override
    public int getSize() {
        return switch (this) {
            case I32, FLOAT -> 32;
            case VOID -> 0;
        };
    }
}
