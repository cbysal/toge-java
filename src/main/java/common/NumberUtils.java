package common;

public final class NumberUtils {
    public static int compare(Number num1, Number num2) {
        if (num1 instanceof Float || num2 instanceof Float)
            return Float.compare(num1.floatValue(), num2.floatValue());
        return Integer.compare(num1.intValue(), num2.intValue());
    }

    public static Number add(Number num1, Number num2) {
        if (num1 instanceof Float || num2 instanceof Float)
            return num1.floatValue() + num2.floatValue();
        return num1.intValue() + num2.intValue();
    }

    public static Number sub(Number num1, Number num2) {
        if (num1 instanceof Float || num2 instanceof Float)
            return num1.floatValue() - num2.floatValue();
        return num1.intValue() - num2.intValue();
    }

    public static Number mul(Number num1, Number num2) {
        if (num1 instanceof Float || num2 instanceof Float)
            return num1.floatValue() * num2.floatValue();
        return num1.intValue() * num2.intValue();
    }

    public static Number div(Number num1, Number num2) {
        if (num1 instanceof Float || num2 instanceof Float)
            return num1.floatValue() / num2.floatValue();
        return num1.intValue() / num2.intValue();
    }

    public static Number mod(Number num1, Number num2) {
        if (num1 instanceof Float || num2 instanceof Float)
            throw new ArithmeticException("float does not support mod operation");
        return num1.intValue() % num2.intValue();
    }

    public static Number lor(Number num1, Number num2) {
        return num1.intValue() != 0 || num2.intValue() != 0 ? 1 : 0;
    }

    public static Number land(Number num1, Number num2) {
        return num1.intValue() != 0 && num2.intValue() != 0 ? 1 : 0;
    }

    public static Number self(Number num) {
        return num;
    }

    public static Number neg(Number num) {
        if (num instanceof Float)
            return -num.floatValue();
        return -num.intValue();
    }

    public static Number lnot(Number num) {
        return num.intValue() == 0 ? 1 : 0;
    }
}
