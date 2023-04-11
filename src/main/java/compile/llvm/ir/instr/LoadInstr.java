package compile.llvm.ir.instr;

import compile.llvm.ir.Value;
import compile.llvm.ir.type.PointerType;

public class LoadInstr extends Instr {
    private final Value src;

    public LoadInstr(Value src) {
        super(((PointerType) src.getType()).base()); // src must be with PointerType
        this.src = src;
    }

    @Override
    public String toString() {
        return String.format("%s = load %s, %s %s, align 4", getTag(), type, src.getType(), src.getTag());
    }
}
