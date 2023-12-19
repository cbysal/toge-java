package compile.llvm.type;

public record PointerType(Type baseType) implements Type {
    @Override
    public int getSize() {
        return 64;
    }

    @Override
    public String toString() {
        return String.format("%s*", baseType);
    }
}
