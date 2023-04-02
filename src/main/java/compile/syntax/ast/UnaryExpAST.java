package compile.syntax.ast;

import compile.symbol.Value;

public class UnaryExpAST implements ExpAST {
    public enum Type {
        F2I, I2F, L_NOT, NEG
    }

    private final Type type;
    private final ExpAST next;

    public UnaryExpAST(Type type, ExpAST next) {
        this.type = type;
        this.next = next;
    }

    @Override
    public Value calc() {
        Value nVal = next.calc();
        return switch (type) {
            case F2I -> {
                if (!nVal.isFloat()) {
                    throw new RuntimeException();
                }
                yield new Value((int) nVal.getFloat());
            }
            case I2F -> {
                if (nVal.isFloat()) {
                    throw new RuntimeException();
                }
                yield new Value((float) nVal.getInt());
            }
            case L_NOT -> {
                if (nVal.isFloat()) {
                    yield new Value(nVal.getFloat() == 0.0f);
                }
                yield new Value(nVal.getInt() == 0);
            }
            case NEG -> {
                if (nVal.isFloat()) {
                    yield new Value(-nVal.getFloat());
                }
                yield new Value(-nVal.getInt());
            }
        };
    }

    public Type getType() {
        return type;
    }

    public ExpAST getNext() {
        return next;
    }

    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + switch (type) {
            case F2I -> "F2IExp";
            case I2F -> "I2FExp";
            case L_NOT -> "LNotExp";
            case NEG -> "NegExp";
        });
        next.print(depth + 1);
    }
}
