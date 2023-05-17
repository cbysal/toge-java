package compile.syntax.ast;

public record IntLitExpAST(int value) implements ExpAST {
    @Override
    public Number calc() {
        return value;
    }

    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "IntLit " + value);
    }
}
