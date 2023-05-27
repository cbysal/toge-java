package compile.syntax.ast;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BinaryOperator;

public record CmpExpAST(CmpExpAST.Type type, ExpAST left, ExpAST right) implements ExpAST {
    public enum Type {
        EQ, NE, GE, GT, LE, LT
    }

    private static final Map<Type, BinaryOperator<Number>> CALC_OPS;

    static {
        Map<Type, BinaryOperator<Number>> calcOps = new HashMap<>();
        calcOps.put(Type.EQ, (val1, val2) -> {
            if (val1 instanceof Integer && val2 instanceof Integer) {
                return val1.intValue() == val2.intValue() ? 1 : 0;
            }
            return Float.compare(val1.floatValue(), val2.floatValue()) == 0 ? 1 : 0;
        });
        calcOps.put(Type.NE, (val1, val2) -> {
            if (val1 instanceof Integer && val2 instanceof Integer) {
                return val1.intValue() != val2.intValue() ? 1 : 0;
            }
            return Float.compare(val1.floatValue(), val2.floatValue()) != 0 ? 1 : 0;
        });
        calcOps.put(Type.GE, (val1, val2) -> {
            if (val1 instanceof Integer && val2 instanceof Integer) {
                return val1.intValue() >= val2.intValue() ? 1 : 0;
            }
            return Float.compare(val1.floatValue(), val2.floatValue()) >= 0 ? 1 : 0;
        });
        calcOps.put(Type.GT, (val1, val2) -> {
            if (val1 instanceof Integer && val2 instanceof Integer) {
                return val1.intValue() > val2.intValue() ? 1 : 0;
            }
            return Float.compare(val1.floatValue(), val2.floatValue()) > 0 ? 1 : 0;
        });
        calcOps.put(Type.LE, (val1, val2) -> {
            if (val1 instanceof Integer && val2 instanceof Integer) {
                return val1.intValue() <= val2.intValue() ? 1 : 0;
            }
            return Float.compare(val1.floatValue(), val2.floatValue()) <= 0 ? 1 : 0;
        });
        calcOps.put(Type.LT, (val1, val2) -> {
            if (val1 instanceof Integer && val2 instanceof Integer) {
                return val1.intValue() < val2.intValue() ? 1 : 0;
            }
            return Float.compare(val1.floatValue(), val2.floatValue()) < 0 ? 1 : 0;
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
        System.out.println("  ".repeat(depth) + "CmpExp " + type);
        left.print(depth + 1);
        right.print(depth + 1);
    }
}
