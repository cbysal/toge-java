package compile.syntax.ast;

import java.util.ArrayList;

public class RootAST extends ArrayList<CompUnitAST> implements AST {
    @Override
    public void print(int depth) {
        System.out.println("Root");
        forEach(compUnit -> compUnit.print(depth + 1));
    }
}
