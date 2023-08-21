package compile.syntax.ast;

public abstract class AST {
    private static int counter = 0;
    private final int id;

    public AST() {
        this.id = counter++;
    }

    public abstract AST copy();

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AST ast = (AST) o;
        return id == ast.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
