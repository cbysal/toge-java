package compile.llvm.ir.instr;

import compile.llvm.ir.Value;
import compile.llvm.ir.type.Type;

public abstract class Instr extends Value {
    private static int autoTag;
    private Instr prev, next;

    Instr(Type type) {
        super(type, String.format("%%t%d", autoTag++));
    }

    public void linkNext(Instr instr) {
        next = instr;
    }

    public void linkPrev(Instr instr) {
        prev = instr;
    }

    public Instr getNext() {
        return next;
    }

    @Override
    public String getTag() {
        return name;
    }
}
