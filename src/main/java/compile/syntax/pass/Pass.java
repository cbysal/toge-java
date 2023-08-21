package compile.syntax.pass;

import compile.syntax.ast.RootAST;

public abstract class Pass {
    protected final RootAST rootAST;

    public Pass(RootAST rootAST) {
        this.rootAST = rootAST;
    }

    public abstract boolean run();
}
