package compile.llvm.ir.type;

public record ArrayType(Type base, int dimension) implements Type {
    @Override
    public String toString() {
        return String.format("[%d x %s]", dimension, base);
    }
}
