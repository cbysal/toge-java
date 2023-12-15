package compile.vir.ir;

import compile.vir.Block;
import compile.vir.type.BasicType;
import compile.vir.value.Value;

public class BranchVIR extends VIR {
    private final Value cond;
    public final Block ifTrue, ifFalse;
    public final Block dest;

    public BranchVIR(Value cond, Block ifTrue, Block ifFalse) {
        super(BasicType.VOID);
        this.cond = cond;
        this.ifTrue = ifTrue;
        this.ifFalse = ifFalse;
        this.dest = null;
    }

    public BranchVIR(Block dest) {
        super(BasicType.VOID);
        this.cond = null;
        this.ifTrue = null;
        this.ifFalse = null;
        this.dest = dest;
    }

    public Value getCond() {
        return cond;
    }

    public boolean conditional() {
        return cond != null;
    }

    @Override
    public String toString() {
        if (conditional())
            return String.format("br i1 %s, label %%%s, label %%%s", cond.getName(), ifTrue, ifFalse);
        return String.format("br label %%%s", dest);
    }
}
