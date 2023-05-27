package compile.syntax.ast;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

public record LNotExpAST(ExpAST next) implements ExpAST {
    @Override
    public Number calc() {
        Number nVal = next.calc();
        return nVal.intValue() == 0 ? 1 : 0;
    }

    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "LNotExp");
        next.print(depth + 1);
    }
}
