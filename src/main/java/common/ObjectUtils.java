package common;

import java.util.Objects;

public final class ObjectUtils {
    public static <T> T checkEquality(T a, T b) {
        if (!Objects.equals(a, b))
            throw new RuntimeException(String.format("Equality check failed: %s != %s", a, b));
        return a;
    }
}
