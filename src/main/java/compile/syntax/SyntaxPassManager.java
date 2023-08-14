package compile.syntax;

import compile.syntax.ast.RootAST;
import compile.syntax.pass.ExpandNestedBlocksPass;
import compile.syntax.pass.RemoveDuplicatedASTsPass;

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
            modified = new ExpandNestedBlocksPass(rootAST).optimize();
            modified |= new RemoveDuplicatedASTsPass(rootAST).optimize();
        } while (modified);
    }
}
