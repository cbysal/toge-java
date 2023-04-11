package compile.llvm.ir.instr;

import compile.llvm.ir.type.PointerType;
import compile.llvm.ir.type.Type;

public class AllocInstr extends Instr {
    public AllocInstr(Type type) {
        super(new PointerType(type));
    }

    @Override
    public String toString() {
        return String.format("%s = alloca %s, align 4", getTag(), ((PointerType) type).base());
    }
}
