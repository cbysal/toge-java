package compile.vir.type;

import java.util.Objects;

public class PointerType implements Type {
    private final Type baseType;

    public PointerType(Type baseType) {
        this.baseType = baseType;
    }

    @Override
    public int getSize() {
        return 64;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PointerType that = (PointerType) o;
        return Objects.equals(baseType, that.baseType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseType);
    }
}
