package compile.syntax.ast;

import compile.symbol.Type;
import compile.symbol.Value;

public class BinaryExpAST implements ExpAST {
    public final Op op;
    public final ExpAST left;
    public final ExpAST right;

    public BinaryExpAST(Op op, ExpAST left, ExpAST right) {
        this.op = op;
        this.left = left;
        this.right = right;
    }

    @Override
    public Value calc() {
        Value lVal = left.calc();
        Value rVal = right.calc();
        return switch (op) {
            case ADD -> {
                if (lVal.getType() == Type.FLOAT && rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.floatValue() + rVal.floatValue());
                if (lVal.getType() == Type.FLOAT)
                    yield new Value(lVal.floatValue() + rVal.intValue());
                if (rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.intValue() + rVal.floatValue());
                yield new Value(lVal.intValue() + rVal.intValue());
            }
            case DIV -> {
                if (lVal.getType() == Type.FLOAT && rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.floatValue() / rVal.floatValue());
                if (lVal.getType() == Type.FLOAT)
                    yield new Value(lVal.floatValue() / rVal.intValue());
                if (rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.intValue() / rVal.floatValue());
                yield new Value(lVal.intValue() / rVal.intValue());
            }
            case MOD -> {
                if (lVal.getType() == Type.FLOAT || rVal.getType() == Type.FLOAT)
                    throw new RuntimeException();
                yield new Value(lVal.intValue() % rVal.intValue());
            }
            case MUL -> {
                if (lVal.getType() == Type.FLOAT && rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.floatValue() * rVal.floatValue());
                if (lVal.getType() == Type.FLOAT)
                    yield new Value(lVal.floatValue() * rVal.intValue());
                if (rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.intValue() * rVal.floatValue());
                yield new Value(lVal.intValue() * rVal.intValue());
            }
            case SUB -> {
                if (lVal.getType() == Type.FLOAT && rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.floatValue() - rVal.floatValue());
                if (lVal.getType() == Type.FLOAT)
                    yield new Value(lVal.floatValue() - rVal.intValue());
                if (rVal.getType() == Type.FLOAT)
                    yield new Value(lVal.intValue() - rVal.floatValue());
                yield new Value(lVal.intValue() - rVal.intValue());
            }
        };
    }

    public enum Op {
        ADD, DIV, MOD, MUL, SUB
    }
}
