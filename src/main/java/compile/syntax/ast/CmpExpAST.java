package compile.syntax.ast;

import compile.symbol.Type;
import compile.symbol.Value;

public class CmpExpAST implements ExpAST {
    public final Op op;
    public ExpAST left;
    public ExpAST right;

    public CmpExpAST(Op op, ExpAST left, ExpAST right) {
        this.op = op;
        this.left = left;
        this.right = right;
    }

    @Override
    public Value calc() {
        Value lVal = left.calc();
        Value rVal = right.calc();
        return switch (op) {
            case EQ -> {
                if (lVal.getType() == Type.FLOAT && rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.floatValue() == rVal.floatValue());
                if (lVal.getType() == Type.FLOAT)
                    yield new Value(lVal.floatValue() == rVal.intValue());
                if (rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.intValue() == rVal.floatValue());
                yield new Value(lVal.intValue() == rVal.intValue());
            }
            case GE -> {
                if (lVal.getType() == Type.FLOAT && rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.floatValue() >= rVal.floatValue());
                if (lVal.getType() == Type.FLOAT)
                    yield new Value(lVal.floatValue() >= rVal.intValue());
                if (rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.intValue() >= rVal.floatValue());
                yield new Value(lVal.intValue() >= rVal.intValue());
            }
            case GT -> {
                if (lVal.getType() == Type.FLOAT && rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.floatValue() > rVal.floatValue());
                if (lVal.getType() == Type.FLOAT)
                    yield new Value(lVal.floatValue() > rVal.intValue());
                if (rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.intValue() > rVal.floatValue());
                yield new Value(lVal.intValue() > rVal.intValue());
            }
            case LE -> {
                if (lVal.getType() == Type.FLOAT && rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.floatValue() <= rVal.floatValue());
                if (lVal.getType() == Type.FLOAT)
                    yield new Value(lVal.floatValue() <= rVal.intValue());
                if (rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.intValue() <= rVal.floatValue());
                yield new Value(lVal.intValue() <= rVal.intValue());
            }
            case LT -> {
                if (lVal.getType() == Type.FLOAT && rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.floatValue() < rVal.floatValue());
                if (lVal.getType() == Type.FLOAT)
                    yield new Value(lVal.floatValue() < rVal.intValue());
                if (rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.intValue() < rVal.floatValue());
                yield new Value(lVal.intValue() < rVal.intValue());
            }
            case NE -> {
                if (lVal.getType() == Type.FLOAT && rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.floatValue() != rVal.floatValue());
                if (lVal.getType() == Type.FLOAT)
                    yield new Value(lVal.floatValue() != rVal.intValue());
                if (rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.intValue() != rVal.floatValue());
                yield new Value(lVal.intValue() != rVal.intValue());
            }
        };
    }

    @Override
    public CmpExpAST copy() {
        return new CmpExpAST(op, left.copy(), right.copy());
    }

    public enum Op {
        EQ, GE, GT, LE, LT, NE
    }
}
