package compile.llvm.type;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class ArrayType implements Type {
    private final Type baseType;
    private final int arraySize;

    public ArrayType(Type baseType, int arraySize) {
        this.baseType = baseType;
        this.arraySize = arraySize;
    }

    @Override
    public int getSize() {
        return baseType.getSize() * arraySize;
    }

    public Type getScalarType() {
        Type type = this;
        while (type instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) type;
            type = arrayType.baseType;
        }
        return type;
    }

    public List<ArrayType> getArrayTypes() {
        List<ArrayType> arrayTypes = new ArrayList<>();
        Type type = this;
        while (type instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) type;
            arrayTypes.add(arrayType);
            type = arrayType.baseType;
        }
        return arrayTypes;
    }

    public List<Integer> getArraySizes() {
        return getArrayTypes().stream().map(ArrayType::arraySize).collect(Collectors.toList());
    }

    @Override
    public Type baseType() {
        return baseType;
    }

    public int arraySize() {
        return arraySize;
    }

    @Override
    public String toString() {
        return String.format("[%d x %s]", arraySize, baseType);
    }
}
