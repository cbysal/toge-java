package compile.llvm.value;

import compile.llvm.type.Type;

import java.util.HashSet;
import java.util.Set;

public abstract class Value {
    protected final Type type;
    private final Set<Use> uses = new HashSet<>();

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

    public void addUse(Use use) {
        uses.add(use);
    }

    public void replaceAllUseAs(Value value) {
        for (Use use : uses) {
            value.uses.add(use);
            use.setValue(value);
        }
        uses.clear();
    }

    public Set<Use> getUses() {
        return uses;
    }
}
