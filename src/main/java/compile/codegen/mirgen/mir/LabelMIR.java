package compile.codegen.mirgen.mir;

import compile.codegen.Label;

public record LabelMIR(Label label) implements MIR {
    public String getName() {
        return label.toString();
    }

    @Override
    public String toString() {
        return label + ":";
    }
}
