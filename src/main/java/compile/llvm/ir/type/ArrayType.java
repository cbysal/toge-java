package compile.llvm.ir.type;

public record ArrayType(Type base, int dimension) implements Type {
    @Override
    public int getSize() {
        return base.getSize() * dimension;
    }

    @Override
    public String toString() {
        return String.format("[%d x %s]", dimension, base);
    }
}
