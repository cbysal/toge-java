package compile.llvm;

import compile.llvm.ir.Instruction;
import compile.llvm.type.Type;
import compile.llvm.value.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;

public class Function extends User {
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

    public void addBlock(BasicBlock block) {
        blocks.add(block);
    }

    public void addBlock(int index, BasicBlock block) {
        blocks.add(index, block);
    }

    public void addBlocks(int index, Collection<BasicBlock> newBlocks) {
        blocks.addAll(index, newBlocks);
    }

    public void insertBlockAfter(BasicBlock base, BasicBlock block) {
        int index = blocks.indexOf(base);
        blocks.add(index + 1, block);
    }

    public List<Argument> getArgs() {
        return args;
    }

    public List<BasicBlock> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<BasicBlock> blocks) {
        this.blocks.clear();
        this.blocks.addAll(blocks);
    }

    @Override
    public String getName() {
        return "@" + name;
    }

    public String getRawName() {
        return name;
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
