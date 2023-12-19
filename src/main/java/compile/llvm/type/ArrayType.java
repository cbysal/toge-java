package compile.llvm.type;

import java.util.ArrayList;
import java.util.List;

public record ArrayType(Type baseType, int arraySize) implements Type {
    @Override
    public int getSize() {
        return baseType.getSize() * arraySize;
    }

    public Type getScalarType() {
        Type type = this;
        while (type instanceof ArrayType arrayType)
            type = arrayType.baseType;
        return type;
    }

    public List<ArrayType> getArrayTypes() {
        List<ArrayType> arrayTypes = new ArrayList<>();
        Type type = this;
        while (type instanceof ArrayType arrayType) {
            arrayTypes.add(arrayType);
            type = arrayType.baseType;
        }
        return arrayTypes;
    }

    public List<Integer> getArraySizes() {
        return getArrayTypes().stream().map(ArrayType::arraySize).toList();
    }

    @Override
    public String toString() {
        return String.format("[%d x %s]", arraySize, baseType);
    }
}
