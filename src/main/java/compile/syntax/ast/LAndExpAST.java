package compile.syntax.ast;

public record LAndExpAST(ExpAST left, ExpAST right) implements ExpAST {
    @Override
    public Number calc() {
        Number lVal = left.calc();
        Number rVal = right.calc();
        return lVal.intValue() != 0 && rVal.intValue() != 0 ? 1 : 0;
    }

    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "LAndExp");
        left.print(depth + 1);
        right.print(depth + 1);
    }
}
