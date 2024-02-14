package compile.llvm;

import compile.codegen.Label;
import compile.llvm.ir.Instruction;
import compile.llvm.type.BasicType;
import compile.llvm.value.Use;
import compile.llvm.value.User;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

public class BasicBlock extends User implements Iterable<Instruction> {
    private static int counter = 0;
    private final int id;
    private final Label label;

    public BasicBlock() {
        super(BasicType.VOID);
        this.id = counter++;
        this.label = new Label();
    }

    public Label getLabel() {
        return label;
    }

    public Instruction getLast() {
        if (operands.isEmpty())
            return null;
        return getLastOperand();
    }

    public void add(Instruction inst) {
        add(new Use(this, inst));
    }

    public void addAll(int index, Collection<? extends Instruction> newInsts) {
        for (Instruction inst : newInsts)
            add(index, new Use(this, inst));
    }

    @Override
    public Iterator<Instruction> iterator() {
        return new Iterator<>() {
            private final Iterator<Use> useIterator = operands.iterator();

            @Override
            public boolean hasNext() {
                return useIterator.hasNext();
            }

            @Override
            public Instruction next() {
                return (Instruction) useIterator.next().value();
            }
        };
    }

    @Override
    public String getName() {
        return "b" + label.getId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        BasicBlock block = (BasicBlock) o;
        return id == block.id && Objects.equals(label, block.label);
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return getName();
    }
}
