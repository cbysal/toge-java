package compile.vir.ir;

import compile.vir.Block;
import compile.vir.type.BasicType;

public class JumpVIR extends VIR {
    public final Block target;

    public JumpVIR(Block target) {
        super(BasicType.VOID);
        this.target = target;
    }

    @Override
    public String toString() {
        return "B       " + target;
    }
}
