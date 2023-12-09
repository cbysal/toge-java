package compile.vir.type;

import java.util.Objects;

public class ArrayType implements Type {
    private final Type baseType;
    private final int arraySize;

    public ArrayType(Type baseType, int arraySize) {
        this.baseType = baseType;
        this.arraySize = arraySize;
    }

    public Type getBaseType() {
        return baseType;
    }

    public int getArraySize() {
        return arraySize;
    }

    @Override
    public int getSize() {
        return baseType.getSize() * arraySize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ArrayType arrayType = (ArrayType) o;
        return arraySize == arrayType.arraySize && Objects.equals(baseType, arrayType.baseType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseType, arraySize);
    }

    @Override
    public String toString() {
        return String.format("[%d x %s]", arraySize, baseType);
    }
}
