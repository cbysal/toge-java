package compile.llvm.ir.type;

import java.util.ArrayList;
import java.util.List;

public record ArrayType(Type base, int dimension) implements Type {
    @Override
    public int getSize() {
        return base.getSize() * dimension;
    }

    public List<Integer> dimensions() {
        List<Integer> dimensions = new ArrayList<>();
        Type type = this;
        while (type instanceof ArrayType arrayType) {
            dimensions.add(arrayType.dimension());
            type = arrayType.base();
        }
        return dimensions;
    }

    @Override
    public BasicType getRootBase() {
        if (base instanceof ArrayType arrayType) {
            return arrayType.base.getRootBase();
        }
        return (BasicType) base;
    }

    @Override
    public boolean equals(Type type) {
        if (type instanceof ArrayType arrayType) {
            if (this.dimension != arrayType.dimension) {
                return false;
            }
            return this.base.equals(this.base);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("[%d x %s]", dimension, base);
    }
}
