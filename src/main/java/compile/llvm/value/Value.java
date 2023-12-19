package compile.llvm.value;

import compile.llvm.type.Type;

import java.util.ArrayList;
import java.util.List;

public abstract class Value {
    protected final Type type;
    private final List<Use> useList = new ArrayList<>();

    public Value(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    abstract public String getName();

    public int getSize() {
        return type.getSize();
    }

    public List<Use> getUseList() {
        return useList;
    }
}
