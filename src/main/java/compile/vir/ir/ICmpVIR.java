package compile.vir.ir;

import compile.vir.type.BasicType;
import compile.vir.type.Type;
import compile.vir.value.Value;

public class ICmpVIR extends VIR {
    private final Cond cond;
    private final Type cmpType;
    private final Value op1, op2;

    public ICmpVIR(Cond cond, Value op1, Value op2) {
        super(BasicType.I1);
        this.cond = cond;
        this.cmpType = op1.getType();
        this.op1 = op1;
        this.op2 = op2;
    }

    public Cond getCond() {
        return cond;
    }

    public Value getOp1() {
        return op1;
    }

    public Value getOp2() {
        return op2;
    }

    @Override
    public String toString() {
        return String.format("%s = icmp %s %s %s, %s", getName(), cond.toString().toLowerCase(), cmpType, op1.getName(), op2.getName());
    }

    public enum Cond {
        EQ, NE, SGT, SGE, SLT, SLE
    }
}
