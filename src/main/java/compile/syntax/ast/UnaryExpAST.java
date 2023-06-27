package compile.syntax.ast;

import compile.symbol.Type;
import compile.symbol.Value;

public record UnaryExpAST(Op op, ExpAST next) implements ExpAST {
    public enum Op {
        F2I, I2F, NEG
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

    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + switch (op) {
            case F2I -> "F2IExp";
            case I2F -> "I2FExp";
            case NEG -> "NegExp";
        });
        next.print(depth + 1);
    }
}
