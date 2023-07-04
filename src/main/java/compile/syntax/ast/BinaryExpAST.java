package compile.syntax.ast;

import compile.symbol.Type;
import compile.symbol.Value;

public record BinaryExpAST(Op op, ExpAST left, ExpAST right) implements ExpAST {
    public enum Op {
        ADD, DIV, MOD, MUL, SUB
    }

    @Override
    public Value calc() {
        Value lVal = left.calc();
        Value rVal = right.calc();
        return switch (op) {
            case ADD -> {
                if (lVal.getType() == Type.FLOAT && rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.getFloat() + rVal.getFloat());
                if (lVal.getType() == Type.FLOAT)
                    yield new Value(lVal.getFloat() + rVal.getInt());
                if (rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.getInt() + rVal.getFloat());
                yield new Value(lVal.getInt() + rVal.getInt());
            }
            case DIV -> {
                if (lVal.getType() == Type.FLOAT && rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.getFloat() / rVal.getFloat());
                if (lVal.getType() == Type.FLOAT)
                    yield new Value(lVal.getFloat() / rVal.getInt());
                if (rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.getInt() / rVal.getFloat());
                yield new Value(lVal.getInt() / rVal.getInt());
            }
            case MOD -> {
                if (lVal.getType() == Type.FLOAT || rVal.getType() == Type.FLOAT)
                    throw new RuntimeException();
                yield new Value(lVal.getInt() % rVal.getInt());
            }
            case MUL -> {
                if (lVal.getType() == Type.FLOAT && rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.getFloat() * rVal.getFloat());
                if (lVal.getType() == Type.FLOAT)
                    yield new Value(lVal.getFloat() * rVal.getInt());
                if (rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.getInt() * rVal.getFloat());
                yield new Value(lVal.getInt() * rVal.getInt());
            }
            case SUB -> {
                if (lVal.getType() == Type.FLOAT && rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.getFloat() - rVal.getFloat());
                if (lVal.getType() == Type.FLOAT)
                    yield new Value(lVal.getFloat() - rVal.getInt());
                if (rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.getInt() - rVal.getFloat());
                yield new Value(lVal.getInt() - rVal.getInt());
            }
        };
    }
}
