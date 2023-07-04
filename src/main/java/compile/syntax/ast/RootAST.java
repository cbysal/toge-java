package compile.syntax.ast;

import java.util.List;

public record RootAST(List<CompUnitAST> compUnits) implements AST {
}
