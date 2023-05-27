package compile.syntax.ast;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BinaryOperator;

public record BinaryExpAST(compile.syntax.ast.BinaryExpAST.Type type, ExpAST left, ExpAST right) implements ExpAST {
    public enum Type {
        ADD, SUB, MUL, DIV, MOD
    }

    private static final Map<Type, BinaryOperator<Number>> CALC_OPS;

    static {
        Map<Type, BinaryOperator<Number>> calcOps = new HashMap<>();
        calcOps.put(Type.ADD, (val1, val2) -> {
            if (val1 instanceof Integer && val2 instanceof Integer) {
                return val1.intValue() + val2.intValue();
            }
            return val1.floatValue() + val2.floatValue();
        });
        calcOps.put(Type.SUB, (val1, val2) -> {
            if (val1 instanceof Integer && val2 instanceof Integer) {
                return val1.intValue() - val2.intValue();
            }
            return val1.floatValue() - val2.floatValue();
        });
        calcOps.put(Type.MUL, (val1, val2) -> {
            if (val1 instanceof Integer && val2 instanceof Integer) {
                return val1.intValue() * val2.intValue();
            }
            return val1.floatValue() * val2.floatValue();
        });
        calcOps.put(Type.DIV, (val1, val2) -> {
            if (val1 instanceof Integer && val2 instanceof Integer) {
                return val1.intValue() / val2.intValue();
            }
            return val1.floatValue() / val2.floatValue();
        });
        calcOps.put(Type.MOD, (val1, val2) -> {
            if (val1 instanceof Integer && val2 instanceof Integer) {
                return val1.intValue() % val2.intValue();
            }
            return val1.floatValue() % val2.floatValue();
        });
        CALC_OPS = calcOps;
    }

    @Override
    public Number calc() {
        Number lVal = left.calc();
        Number rVal = right.calc();
        return CALC_OPS.get(type).apply(lVal, rVal);
    }

    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "BinaryExp " + type);
        left.print(depth + 1);
        right.print(depth + 1);
    }
}
