package compile.syntax.ast;

import compile.symbol.Type;
import compile.symbol.Value;

public record CmpExpAST(Op op, ExpAST left, ExpAST right) implements ExpAST {
    public enum Op {
        EQ, GE, GT, LE, LT, NE
    }

    @Override
    public Value calc() {
        Value lVal = left.calc();
        Value rVal = right.calc();
        return switch (op) {
            case EQ -> {
                if (lVal.getType() == Type.FLOAT && rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.getFloat() == rVal.getFloat());
                if (lVal.getType() == Type.FLOAT)
                    yield new Value(lVal.getFloat() == rVal.getInt());
                if (rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.getInt() == rVal.getFloat());
                yield new Value(lVal.getInt() == rVal.getInt());
            }
            case GE -> {
                if (lVal.getType() == Type.FLOAT && rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.getFloat() >= rVal.getFloat());
                if (lVal.getType() == Type.FLOAT)
                    yield new Value(lVal.getFloat() >= rVal.getInt());
                if (rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.getInt() >= rVal.getFloat());
                yield new Value(lVal.getInt() >= rVal.getInt());
            }
            case GT -> {
                if (lVal.getType() == Type.FLOAT && rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.getFloat() > rVal.getFloat());
                if (lVal.getType() == Type.FLOAT)
                    yield new Value(lVal.getFloat() > rVal.getInt());
                if (rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.getInt() > rVal.getFloat());
                yield new Value(lVal.getInt() > rVal.getInt());
            }
            case LE -> {
                if (lVal.getType() == Type.FLOAT && rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.getFloat() <= rVal.getFloat());
                if (lVal.getType() == Type.FLOAT)
                    yield new Value(lVal.getFloat() <= rVal.getInt());
                if (rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.getInt() <= rVal.getFloat());
                yield new Value(lVal.getInt() <= rVal.getInt());
            }
            case LT -> {
                if (lVal.getType() == Type.FLOAT && rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.getFloat() < rVal.getFloat());
                if (lVal.getType() == Type.FLOAT)
                    yield new Value(lVal.getFloat() < rVal.getInt());
                if (rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.getInt() < rVal.getFloat());
                yield new Value(lVal.getInt() < rVal.getInt());
            }
            case NE -> {
                if (lVal.getType() == Type.FLOAT && rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.getFloat() != rVal.getFloat());
                if (lVal.getType() == Type.FLOAT)
                    yield new Value(lVal.getFloat() != rVal.getInt());
                if (rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.getInt() != rVal.getFloat());
                yield new Value(lVal.getInt() != rVal.getInt());
            }
        };
    }

    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + switch (op) {
            case EQ -> "EqExp";
            case LE -> "LeExp";
            case LT -> "LtExp";
            case GE -> "GeExp";
            case GT -> "GtExp";
            case NE -> "NeExp";
        });
        left.print(depth + 1);
        right.print(depth + 1);
    }
}
