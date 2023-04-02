package compile.symbol;

import java.util.List;
import java.util.Map;

public class ConstSymbol extends InitializedDataSymbol {

    ConstSymbol(boolean isFloat, String name, float value) {
        super(isFloat, name, value);
    }

    ConstSymbol(boolean isFloat, String name, int value) {
        super(isFloat, name, value);
    }

    ConstSymbol(boolean isFloat, String name, List<Integer> dimensions, Map<Integer, Integer> values) {
        super(isFloat, name, dimensions, values);
    }

    @Override
    public String toString() {
        return "const " + super.toString();
    }
}
