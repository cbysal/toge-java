package compile.vir.ir;

import compile.vir.type.BasicType;
import compile.vir.value.Value;

public class UnaryVIR extends VIR {
    public final Type type;
    public final Value source;

    public UnaryVIR(Type type, Value source) {
        super(switch (type) {
            case I2F -> BasicType.FLOAT;
            case FNEG, ABS -> source.getType();
        });
        this.type = type;
        this.source = source;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(type);
        builder.append(" ".repeat(8 - builder.length()));
        builder.append(getName()).append(", ").append(source instanceof VIR ir ? ir.getName() : source);
        return builder.toString();
    }

    public enum Type {
        I2F, FNEG, ABS
    }
}
