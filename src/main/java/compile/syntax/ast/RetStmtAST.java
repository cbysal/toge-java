package compile.syntax.ast;

import java.util.Objects;

public class RetStmtAST implements StmtAST {
    private final ExpAST value;

    public RetStmtAST() {
        this.value = null;
    }

    public RetStmtAST(ExpAST value) {
        this.value = value;
    }

    public ExpAST getValue() {
        return Objects.requireNonNull(value);
    }

    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "RetStmt");
        if (value != null) {
            value.print(depth + 1);
        }
    }
}
