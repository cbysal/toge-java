package compile.llvm.ir;

import compile.llvm.type.Type;
import compile.llvm.value.Use;
import compile.llvm.value.User;
import compile.llvm.value.Value;

public abstract class Instruction extends User {
    private static int counter = 0;
    protected final int id;

    protected Instruction(Type type, Value... operands) {
        super(type);
        for (Value operand : operands)
            add(new Use(this, operand));
        this.id = counter++;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public final String getName() {
        return "%v" + id;
    }
}
