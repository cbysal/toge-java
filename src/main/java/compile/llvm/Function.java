package compile.llvm;

import compile.llvm.ir.Instruction;
import compile.llvm.type.Type;
import compile.llvm.value.Value;

import java.util.*;

public class Function extends Value implements Iterable<BasicBlock> {
    private final String name;
    private final List<Argument> args = new ArrayList<>();
    private final List<BasicBlock> blocks = new ArrayList<>();

    public Function(Type type, String name) {
        super(type);
        this.name = name;
    }

    public boolean isDeclare() {
        return blocks.isEmpty();
    }

    public Function addArg(Argument arg) {
        args.add(arg);
        return this;
    }

    public boolean add(BasicBlock block) {
        return blocks.add(block);
    }

    public void add(int index, BasicBlock block) {
        blocks.add(index, block);
    }

    public boolean add(int index, Collection<BasicBlock> newBlocks) {
        return blocks.addAll(index, newBlocks);
    }

    public int size() {
        return blocks.size();
    }

    public BasicBlock get(int index) {
        return blocks.get(index);
    }

    public BasicBlock getFirst() {
        return blocks.getFirst();
    }

    public void insertAfter(BasicBlock base, BasicBlock block) {
        int index = blocks.indexOf(base);
        blocks.add(index + 1, block);
    }

    public List<Argument> getArgs() {
        return args;
    }

    @Override
    public String getName() {
        return "@" + name;
    }

    public String getRawName() {
        return name;
    }

    @Override
    public Iterator<BasicBlock> iterator() {
        return blocks.iterator();
    }

    @Override
    public String toString() {
        boolean isDeclare = blocks.isEmpty();
        StringBuilder builder = new StringBuilder();
        StringJoiner joiner = new StringJoiner(", ", "(", ")");
        for (Argument arg : args) {
            if (isDeclare)
                joiner.add(arg.getType().toString());
            else
                joiner.add(arg.toString());
        }
        if (isDeclare)
            builder.append("declare ");
        else
            builder.append("define ");
        builder.append(String.format("%s %s", type, getName())).append(joiner);
        if (isDeclare)
            return builder.append('\n').toString();
        builder.append(" {\n");
        for (BasicBlock block : blocks) {
            builder.append(block).append(":\n");
            for (Instruction inst : block)
                builder.append("  ").append(inst).append('\n');
        }
        builder.append("}\n");
        return builder.toString();
    }
}
