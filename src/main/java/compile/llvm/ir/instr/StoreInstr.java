package compile.llvm.ir.instr;

import compile.llvm.ir.Value;
import compile.llvm.ir.type.BasicType;

public class StoreInstr extends Instr {
    private final Value src;
    private final Value dst;

    public StoreInstr(Value src, Value dst) {
        super(BasicType.VOID);
        this.src = src;
        this.dst = dst;
        if (src instanceof Instr instr) {
            instr.addUse(this);
        }
        if (dst instanceof Instr instr) {
            instr.addUse(this);
        }
    }

    public Value getSrc() {
        return src;
    }

    public Value getDst() {
        return dst;
    }

    @Override
    public String getTag() {
        throw new RuntimeException("The return type of Store is always VOID. Never call this method!");
    }

    @Override
    public String toString() {
        return String.format("store %s %s, %s %s, align 4", src.getType(), src.getTag(), dst.getType(), dst.getTag());
    }
}
