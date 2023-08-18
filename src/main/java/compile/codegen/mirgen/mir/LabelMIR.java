package compile.codegen.mirgen.mir;

import compile.codegen.Label;

public class LabelMIR extends MIR {
    public final Label label;

    public LabelMIR(Label label) {
        this.label = label;
    }

    public String getName() {
        return label.toString();
    }

    @Override
    public String toString() {
        return label + ":";
    }
}
