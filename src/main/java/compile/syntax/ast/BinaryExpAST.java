package compile.syntax.ast;

import compile.symbol.Value;

public record BinaryExpAST(compile.syntax.ast.BinaryExpAST.Type type, ExpAST left, ExpAST right) implements ExpAST {
    public enum Type {
        L_OR, L_AND, EQ, NE, GE, GT, LE, LT, ADD, SUB, DIV, MOD, MUL
    }

    @Override
    public Value calc() {
        Value lVal = left.calc();
        Value rVal = right.calc();
        return switch (type) {
            case L_OR -> {
                if (lVal.isFloat() && rVal.isFloat()) {
                    yield new Value(lVal.getFloat() != 0 || rVal.getFloat() != 0);
                }
                if (lVal.isFloat()) {
                    yield new Value(lVal.getFloat() != 0 || rVal.getInt() != 0);
                }
                if (rVal.isFloat()) {
                    yield new Value(lVal.getInt() != 0 || rVal.getFloat() != 0);
                }
                yield new Value(lVal.getInt() != 0 || rVal.getInt() != 0);
            }
            case L_AND -> {
                if (lVal.isFloat() && rVal.isFloat()) {
                    yield new Value(lVal.getFloat() != 0 && rVal.getFloat() != 0);
                }
                if (lVal.isFloat()) {
                    yield new Value(lVal.getFloat() != 0 && rVal.getInt() != 0);
                }
                if (rVal.isFloat()) {
                    yield new Value(lVal.getInt() != 0 && rVal.getFloat() != 0);
                }
                yield new Value(lVal.getInt() != 0 && rVal.getInt() != 0);
            }
            case EQ -> {
                if (lVal.isFloat() && rVal.isFloat()) {
                    yield new Value(lVal.getFloat() == rVal.getFloat());
                }
                if (lVal.isFloat()) {
                    yield new Value(lVal.getFloat() == rVal.getInt());
                }
                if (rVal.isFloat()) {
                    yield new Value(lVal.getInt() == rVal.getFloat());
                }
                yield new Value(lVal.getInt() == rVal.getInt());
            }
            case NE -> {
                if (lVal.isFloat() && rVal.isFloat()) {
                    yield new Value(lVal.getFloat() != rVal.getFloat());
                }
                if (lVal.isFloat()) {
                    yield new Value(lVal.getFloat() != rVal.getInt());
                }
                if (rVal.isFloat()) {
                    yield new Value(lVal.getInt() != rVal.getFloat());
                }
                yield new Value(lVal.getInt() != rVal.getInt());
            }
            case GE -> {
                if (lVal.isFloat() && rVal.isFloat()) {
                    yield new Value(lVal.getFloat() >= rVal.getFloat());
                }
                if (lVal.isFloat()) {
                    yield new Value(lVal.getFloat() >= rVal.getInt());
                }
                if (rVal.isFloat()) {
                    yield new Value(lVal.getInt() >= rVal.getFloat());
                }
                yield new Value(lVal.getInt() >= rVal.getInt());
            }
            case GT -> {
                if (lVal.isFloat() && rVal.isFloat()) {
                    yield new Value(lVal.getFloat() > rVal.getFloat());
                }
                if (lVal.isFloat()) {
                    yield new Value(lVal.getFloat() > rVal.getInt());
                }
                if (rVal.isFloat()) {
                    yield new Value(lVal.getInt() > rVal.getFloat());
                }
                yield new Value(lVal.getInt() > rVal.getInt());
            }
            case LE -> {
                if (lVal.isFloat() && rVal.isFloat()) {
                    yield new Value(lVal.getFloat() <= rVal.getFloat());
                }
                if (lVal.isFloat()) {
                    yield new Value(lVal.getFloat() <= rVal.getInt());
                }
                if (rVal.isFloat()) {
                    yield new Value(lVal.getInt() <= rVal.getFloat());
                }
                yield new Value(lVal.getInt() <= rVal.getInt());
            }
            case LT -> {
                if (lVal.isFloat() && rVal.isFloat()) {
                    yield new Value(lVal.getFloat() < rVal.getFloat());
                }
                if (lVal.isFloat()) {
                    yield new Value(lVal.getFloat() < rVal.getInt());
                }
                if (rVal.isFloat()) {
                    yield new Value(lVal.getInt() < rVal.getFloat());
                }
                yield new Value(lVal.getInt() < rVal.getInt());
            }
            case ADD -> {
                if (lVal.isFloat() && rVal.isFloat()) {
                    yield new Value(lVal.getFloat() + rVal.getFloat());
                }
                if (lVal.isFloat()) {
                    yield new Value(lVal.getFloat() + rVal.getInt());
                }
                if (rVal.isFloat()) {
                    yield new Value(lVal.getInt() + rVal.getFloat());
                }
                yield new Value(lVal.getInt() + rVal.getInt());
            }
            case SUB -> {
                if (lVal.isFloat() && rVal.isFloat()) {
                    yield new Value(lVal.getFloat() - rVal.getFloat());
                }
                if (lVal.isFloat()) {
                    yield new Value(lVal.getFloat() - rVal.getInt());
                }
                if (rVal.isFloat()) {
                    yield new Value(lVal.getInt() - rVal.getFloat());
                }
                yield new Value(lVal.getInt() - rVal.getInt());
            }
            case MUL -> {
                if (lVal.isFloat() && rVal.isFloat()) {
                    yield new Value(lVal.getFloat() * rVal.getFloat());
                }
                if (lVal.isFloat()) {
                    yield new Value(lVal.getFloat() * rVal.getInt());
                }
                if (rVal.isFloat()) {
                    yield new Value(lVal.getInt() * rVal.getFloat());
                }
                yield new Value(lVal.getInt() * rVal.getInt());
            }
            case DIV -> {
                if (lVal.isFloat() && rVal.isFloat()) {
                    yield new Value(lVal.getFloat() / rVal.getFloat());
                }
                if (lVal.isFloat()) {
                    yield new Value(lVal.getFloat() / rVal.getInt());
                }
                if (rVal.isFloat()) {
                    yield new Value(lVal.getInt() / rVal.getFloat());
                }
                yield new Value(lVal.getInt() / rVal.getInt());
            }
            case MOD -> {
                if (lVal.isFloat() || rVal.isFloat()) {
                    throw new RuntimeException();
                }
                yield new Value(lVal.getInt() % rVal.getInt());
            }
        };
    }

    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "BinaryExp " + type);
        left.print(depth + 1);
        right.print(depth + 1);
    }
}
