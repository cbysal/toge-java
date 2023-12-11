package compile.symbol;

import compile.vir.Argument;
import compile.vir.type.Type;

import java.util.ArrayList;
import java.util.List;

public class FuncSymbol extends Symbol {
    private final List<Argument> args = new ArrayList<>();

    FuncSymbol(Type type, String name) {
        super(type, name);
    }

    public void addArg(Argument arg) {
        args.add(arg);
    }

    public List<Argument> getArgs() {
        return args;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(type.toString().toLowerCase()).append(' ').append(name).append('(');
        boolean isFirst = true;
        for (Argument arg : args) {
            if (!isFirst)
                builder.append(", ");
            isFirst = false;
            builder.append(arg);
        }
        builder.append(')');
        return builder.toString();
    }
}
