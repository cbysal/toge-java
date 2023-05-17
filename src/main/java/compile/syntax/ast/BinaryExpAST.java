package compile.syntax.ast;

public record BinaryExpAST(compile.syntax.ast.BinaryExpAST.Type type, ExpAST left, ExpAST right) implements ExpAST {
    public enum Type {
        L_OR, L_AND, EQ, NE, GE, GT, LE, LT, ADD, SUB, MUL, DIV, MOD
    }

    @Override
    public Number calc() {
        Number lVal = left.calc();
        Number rVal = right.calc();
        return switch (type) {
            case L_OR -> lVal.intValue() != 0 || rVal.intValue() != 0 ? 1 : 0;
            case L_AND -> lVal.intValue() != 0 && rVal.intValue() != 0 ? 1 : 0;
            case EQ -> {
                if (lVal instanceof Integer && rVal instanceof Integer) {
                    yield lVal.intValue() == rVal.intValue() ? 1 : 0;
                }
                yield lVal.floatValue() == rVal.floatValue() ? 1 : 0;
            }
            case NE -> {
                if (lVal instanceof Integer && rVal instanceof Integer) {
                    yield lVal.intValue() != rVal.intValue() ? 1 : 0;
                }
                yield Float.compare(lVal.floatValue(), rVal.floatValue()) != 0 ? 1 : 0;
            }
            case GE -> {
                if (lVal instanceof Integer && rVal instanceof Integer) {
                    yield lVal.intValue() >= rVal.intValue() ? 1 : 0;
                }
                yield Float.compare(lVal.floatValue(), rVal.floatValue()) >= 0 ? 1 : 0;
            }
            case GT -> {
                if (lVal instanceof Integer && rVal instanceof Integer) {
                    yield lVal.intValue() > rVal.intValue() ? 1 : 0;
                }
                yield Float.compare(lVal.floatValue(), rVal.floatValue()) > 0 ? 1 : 0;
            }
            case LE -> {
                if (lVal instanceof Integer && rVal instanceof Integer) {
                    yield lVal.intValue() <= rVal.intValue() ? 1 : 0;
                }
                yield Float.compare(lVal.floatValue(), rVal.floatValue()) <= 0 ? 1 : 0;
            }
            case LT -> {
                if (lVal instanceof Integer && rVal instanceof Integer) {
                    yield lVal.intValue() < rVal.intValue() ? 1 : 0;
                }
                yield Float.compare(lVal.floatValue(), rVal.floatValue()) < 0 ? 1 : 0;
            }
            case ADD -> {
                if (lVal instanceof Integer && rVal instanceof Integer) {
                    yield lVal.intValue() + rVal.intValue();
                }
                yield lVal.floatValue() + rVal.floatValue();
            }
            case SUB -> {
                if (lVal instanceof Integer && rVal instanceof Integer) {
                    yield lVal.intValue() - rVal.intValue();
                }
                yield lVal.floatValue() - rVal.floatValue();
            }
            case MUL -> {
                if (lVal instanceof Integer && rVal instanceof Integer) {
                    yield lVal.intValue() * rVal.intValue();
                }
                yield lVal.floatValue() * rVal.floatValue();
            }
            case DIV -> {
                if (lVal instanceof Integer && rVal instanceof Integer) {
                    yield lVal.intValue() / rVal.intValue();
                }
                yield lVal.floatValue() / rVal.floatValue();
            }
            case MOD -> {
                if (lVal instanceof Integer && rVal instanceof Integer) {
                    yield lVal.intValue() % rVal.intValue();
                }
                yield lVal.floatValue() % rVal.floatValue();
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
