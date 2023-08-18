package compile.syntax.ast;

import compile.symbol.Type;
import compile.symbol.Value;

public class UnaryExpAST implements ExpAST {
    public final Op op;
    public final ExpAST next;

    public UnaryExpAST(Op op, ExpAST next) {
        this.op = op;
        this.next = next;
    }

    @Override
    public Value calc() {
        Value nVal = next.calc();
        return switch (op) {
            case F2I -> {
                if (nVal.getType() != Type.FLOAT)
                    throw new RuntimeException();
                yield new Value((int) nVal.getFloat());
            }
            case I2F -> {
                if (nVal.getType() != Type.INT)
                    throw new RuntimeException();
                yield new Value((float) nVal.getInt());
            }
            case NEG -> {
                if (nVal.getType() == Type.FLOAT)
                    yield new Value(-nVal.getFloat());
                yield new Value(-nVal.getInt());
            }
        };
    }

    public enum Op {
        F2I, I2F, NEG
    }
}
