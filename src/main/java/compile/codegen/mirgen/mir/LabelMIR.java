package compile.codegen.mirgen.mir;

import compile.codegen.Label;

public class LabelMIR implements MIR {
    private final Label label;

    public LabelMIR(Label label) {
        this.label = label;
    }

    public Label getLabel() {
        return label;
    }

    public String getName() {
        return label.toString();
    }

    @Override
    public String toString() {
        return label + ":";
    }
}
