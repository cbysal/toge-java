package compile.syntax.ast;

public record FloatLitExpAST(float value) implements ExpAST {
    @Override
    public Number calc() {
        return value;
    }

    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "FloatLit " + value);
    }
}
