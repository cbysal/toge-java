package compile.vir;

import compile.vir.contant.Constant;
import compile.vir.contant.ConstantArray;
import compile.vir.contant.ConstantNumber;
import compile.vir.contant.ConstantZero;
import compile.vir.type.ArrayType;
import compile.vir.type.BasicType;
import compile.vir.type.PointerType;
import compile.vir.type.Type;
import compile.vir.value.User;

import java.util.ArrayList;
import java.util.List;

public class GlobalVariable extends User {
    private final boolean isConst;
    private final String name;
    private final Constant value;

    public GlobalVariable(boolean isConst, Type type, String name, Constant value) {
        super(type);
        this.isConst = isConst;
        this.name = name;
        this.value = value;
    }

    public boolean isConst() {
        return isConst;
    }

    public List<Integer> getDimensions() {
        List<Integer> dimensions = new ArrayList<>();
        Type type = this.type;
        while (type instanceof ArrayType arrayType) {
            dimensions.add(arrayType.getArraySize());
            type = arrayType.getBaseType();
        }
        return dimensions;
    }

    public int[] getSizes() {
        List<Integer> dimensions = getDimensions();
        int[] sizes = new int[dimensions.size()];
        for (int i = dimensions.size() - 1; i > 0; i--)
            sizes[i - 1] = sizes[i] * dimensions.get(i);
        return sizes;
    }

    public boolean isSingle() {
        return type instanceof BasicType;
    }

    public boolean isInBss() {
        return value instanceof ConstantZero;
    }

    public float getFloat() {
        if (type instanceof BasicType) {
            return ((ConstantNumber) value).floatValue();
        }
        throw new RuntimeException();
    }

    public float getFloat(int index) {
        List<Integer> dimensions = getDimensions();
        Constant value = this.value;
        for (int dimension : dimensions) {
            switch (value) {
                case ConstantArray constantArray -> {
                    value = constantArray.getValues().get(index / dimension);
                    index %= dimension;
                }
                case ConstantZero constantZero -> {
                    return 0;
                }
                default -> throw new IllegalStateException("Unexpected value: " + value);
            }
        }
        return ((ConstantNumber) value).floatValue();
    }

    public int getInt() {
        if (type instanceof BasicType) {
            return ((ConstantNumber) value).intValue();
        }
        throw new RuntimeException();
    }

    public int getInt(int index) {
        List<Integer> dimensions = new ArrayList<>(getDimensions());
        dimensions.removeFirst();
        dimensions.add(1);
        Constant value = this.value;
        for (int i = 0; i < dimensions.size(); i++) {
            int dimension = dimensions.get(i);
            switch (value) {
                case ConstantArray constantArray -> {
                    value = constantArray.getValues().get(index / dimensions.stream().skip(i).reduce(1, Math::multiplyExact));
                    index %= dimension;
                }
                case ConstantZero constantZero -> {
                    return 0;
                }
                default -> throw new IllegalStateException("Unexpected value: " + value);
            }
        }
        return ((ConstantNumber) value).intValue();
    }

    @Override
    public String getName() {
        return "@" + name;
    }

    public String getRawName() {
        return name;
    }

    @Override
    public String toString() {
        return String.format("%s = global %s", getName(), value);
    }
}
