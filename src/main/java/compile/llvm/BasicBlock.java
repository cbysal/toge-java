package compile.llvm;

import compile.llvm.ir.Instruction;
import compile.llvm.type.BasicType;
import compile.llvm.value.Value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class BasicBlock extends Value implements Iterable<Instruction> {
    private static int counter = 0;
    private final int id;
    private final Function function;
    private final List<Instruction> instructions = new ArrayList<>();

    public BasicBlock(Function function) {
        super(BasicType.VOID);
        this.function = function;
        this.id = counter++;
    }

    public boolean isEmpty() {
        return instructions.isEmpty();
    }

    public Instruction getLast() {
        return instructions.getLast();
    }

    public boolean add(Instruction inst) {
        return instructions.add(inst);
    }

    public void add(int index, Instruction inst) {
        instructions.add(index, inst);
    }

    public void addAll(int index, Collection<? extends Instruction> newInsts) {
        instructions.addAll(index, newInsts);
    }

    public Instruction remove(int index) {
        return instructions.remove(index);
    }

    public Instruction get(int index) {
        return instructions.get(index);
    }

    public int size() {
        return instructions.size();
    }

    @Override
    public Iterator<Instruction> iterator() {
        return instructions.iterator();
    }

    @Override
    public String getName() {
        return "b" + id;
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
