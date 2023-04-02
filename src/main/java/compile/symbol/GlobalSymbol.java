package compile.symbol;

import java.util.List;
import java.util.Map;

public class GlobalSymbol extends InitializedDataSymbol {

    GlobalSymbol(boolean isFloat, String name, float value) {
        super(isFloat, name, value);
    }

    GlobalSymbol(boolean isFloat, String name, int value) {
        super(isFloat, name, value);
    }

    GlobalSymbol(boolean isFloat, String name, List<Integer> dimensions, Map<Integer, Integer> values) {
        super(isFloat, name, dimensions, values);
    }

    @Override
    public String toString() {
        return "global " + super.toString();
    }
}
