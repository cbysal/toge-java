package compile.syntax.ast;

import compile.symbol.Value;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

public record InitValAST(List<ExpAST> exps) implements ExpAST, Iterable<ExpAST> {
    @Override
    public Value calc() {
        return null;
    }

    public boolean isEmpty() {
        return exps.isEmpty();
    }

    public ExpAST get(int index) {
        return exps.get(index);
    }

    @Override
    public void print(int depth) {
        throw new RuntimeException();
    }

    @Override
    public Iterator<ExpAST> iterator() {
        return exps.iterator();
    }

    @Override
    public void forEach(Consumer<? super ExpAST> action) {
        exps.forEach(action);
    }

    @Override
    public Spliterator<ExpAST> spliterator() {
        return exps.spliterator();
    }
}
