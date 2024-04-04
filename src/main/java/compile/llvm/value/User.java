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
        use.getValue().addUse(use);
    }

    public Use remove(int index) {
        operands.get(index).getValue().getUses().remove(operands.get(index));
        return operands.remove(index);
    }

    public Use remove(Value value) {
        for (int i = 0; i < operands.size(); i++) {
            if (operands.get(i).getValue() == value) {
                return operands.remove(i);
            }
        }
        return null;
    }

    public Use get(int index) {
        return operands.get(index);
    }

    public <T extends Value> T getLastOperand() {
        return getOperand(size() - 1);
    }

    public <T extends Value> T getOperand(int index) {
        return (T) get(index).getValue();
    }

    public boolean isEmpty() {
        return operands.isEmpty();
    }

    public void set(int index, Use use) {
        while (operands.size() <= index)
            operands.add(null);
        operands.set(index, use);
        use.getValue().addUse(use);
    }

    public int size() {
        return operands.size();
    }
}
