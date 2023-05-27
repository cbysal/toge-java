package compile.llvm.ir.type;

public record PointerType(Type base) implements Type {
    @Override
    public int getSize() {
        return 8;
    }

    @Override
    public BasicType getRootBase() {
        if (base instanceof PointerType pointerType) {
            return pointerType.base().getRootBase();
        }
        if (base instanceof ArrayType arrayType) {
            return arrayType.base().getRootBase();
        }
        return (BasicType) base;
    }

    @Override
    public boolean equals(Type type) {
        if (type instanceof PointerType pointerType) {
            return this.base.equals(pointerType.base);
        }
        return false;
    }

    @Override
    public String toString() {
        return base + "*";
    }
}
