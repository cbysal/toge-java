package compile.syntax.ast;

public record UnaryExpAST(compile.syntax.ast.UnaryExpAST.Type type, ExpAST next) implements ExpAST {
    public enum Type {
        F2I, I2F, L_NOT, NEG
    }

    @Override
    public Number calc() {
        Number nVal = next.calc();
        return switch (type) {
            case F2I -> nVal.intValue();
            case I2F -> nVal.floatValue();
            case L_NOT -> nVal.intValue() == 0 ? 1 : 0;
            case NEG -> {
                if (nVal instanceof Integer) {
                    yield -nVal.intValue();
                }
                yield -nVal.floatValue();
            }
        };
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
