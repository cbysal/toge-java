package compile.llvm.value;

import compile.llvm.type.Type;

import java.util.ArrayList;
import java.util.List;

public abstract class User extends Value {
    protected final List<Use> operands = new ArrayList<>();

    public User(Type type) {
        super(type);
    }

    public void add(Use use) {
        add(size(), use);
    }

    public void add(int index, Use use) {
        operands.add(index, use);
        use.value().addUse(use);
    }

    public Use get(int index) {
        return operands.get(index);
    }

    public <T extends Value> T getLastOperand() {
        return getOperand(size() - 1);
    }

    public <T extends Value> T getOperand(int index) {
        return (T) get(index).value();
    }

    public boolean isEmpty() {
        return operands.isEmpty();
    }

    public void set(int index, Use use) {
        while (operands.size() <= index)
            operands.add(null);
        operands.set(index, use);
        use.value().addUse(use);
    }

    public int size() {
        return operands.size();
    }
}
