package compile.syntax.ast;

import java.util.List;

public record RootAST(List<CompUnitAST> compUnits) implements AST {
    @Override
    public void print(int depth) {
        System.out.println("Root");
        compUnits.forEach(compUnit -> compUnit.print(depth + 1));
    }
}
