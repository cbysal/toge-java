package compile.llvm.ir;

import compile.llvm.GlobalVariable;
import compile.llvm.type.PointerType;
import compile.llvm.value.Value;

public class LoadInst extends Instruction {
    public final Value pointer;

    public LoadInst(Value pointer) {
        super(switch (pointer) {
            case GlobalVariable global -> pointer.getType();
            default -> pointer.getType().baseType();
        });
        this.pointer = pointer;
    }

    @Override
    public String toString() {
        return String.format("%s = load %s, %s %s", getName(), type, switch (pointer) {
            case GlobalVariable global -> new PointerType(pointer.getType());
            default -> pointer.getType();
        }, pointer.getName());
    }
}
