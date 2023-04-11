package compile.llvm.ir.type;

public record PointerType(Type base) implements Type {
    @Override
    public String toString() {
        return base + "*";
    }
}
