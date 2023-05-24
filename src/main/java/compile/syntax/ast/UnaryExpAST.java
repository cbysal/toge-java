package compile.syntax.ast;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

public record UnaryExpAST(compile.syntax.ast.UnaryExpAST.Type type, ExpAST next) implements ExpAST {
    public enum Type {
        F2I, I2F, L_NOT, NEG
    }

    private static final Map<Type, UnaryOperator<Number>> CALC_OPS;

    static {
        Map<Type, UnaryOperator<Number>> calcOps = new HashMap<>();
        calcOps.put(Type.F2I, Number::intValue);
        calcOps.put(Type.I2F, Number::floatValue);
        calcOps.put(Type.L_NOT, val -> val.intValue() == 0 ? 1 : 0);
        calcOps.put(Type.NEG, val -> {
            if (val instanceof Integer) {
                return -val.intValue();
            }
            return -val.floatValue();
        });
        CALC_OPS = calcOps;
    }

    @Override
    public Number calc() {
        Number nVal = next.calc();
        return CALC_OPS.get(type).apply(nVal);
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
