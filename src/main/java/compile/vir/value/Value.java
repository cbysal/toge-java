package compile.vir.value;

import compile.vir.type.Type;

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

    public int getSize() {
        return type.getSize();
    }

    public List<Use> getUseList() {
        return useList;
    }
}
