package compile.llvm.type;

public final class PointerType implements Type {
    private final Type baseType;

    public PointerType(Type baseType) {
        this.baseType = baseType;
    }

    public Type baseType() {
        return baseType;
    }

    @Override
    public int getSize() {
        return 64;
    }

    @Override
    public String toString() {
        return String.format("%s*", baseType);
    }
}