package compile.llvm.ir.type;

public record PointerType(Type base) implements Type {
    @Override
    public int getSize() {
        return 8;
    }

    @Override
    public String toString() {
        return base + "*";
    }
}
