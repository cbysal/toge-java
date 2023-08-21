package compile.syntax;

import compile.syntax.ast.RootAST;
import compile.syntax.pass.ConstantFolding;
import compile.syntax.pass.ExpandNestedBlocksPass;
import compile.syntax.pass.RemoveDuplicatedASTsPass;
import compile.syntax.pass.UnrollStandardFor;

public class SyntaxPassManager {
    private final RootAST rootAST;

    public SyntaxPassManager(RootAST rootAST) {
        this.rootAST = rootAST;
    }

    public RootAST getRootAST() {
        return rootAST;
    }

    public void run() {
        boolean modified;
        do {
            modified = new ExpandNestedBlocksPass(rootAST).run();
            modified |= new RemoveDuplicatedASTsPass(rootAST).run();
            modified |= new ConstantFolding(rootAST).run();
            modified |= new UnrollStandardFor(rootAST).run();
        } while (modified);
    }
}
