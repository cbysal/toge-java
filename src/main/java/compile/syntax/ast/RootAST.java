package compile.syntax.ast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class RootAST extends AST implements Iterable<CompUnitAST> {
    private final List<CompUnitAST> stmts = new ArrayList<>();

    @Override
    public Iterator<CompUnitAST> iterator() {
        return stmts.iterator();
    }

    public boolean add(CompUnitAST e) {
        return this.stmts.add(e);
    }

    public boolean addAll(Collection<? extends CompUnitAST> c) {
        return this.stmts.addAll(c);
    }
}
