package compile.vir;

import compile.vir.ir.VIR;
import compile.vir.type.Type;
import compile.vir.value.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;

public class VirtualFunction extends User {
    private final String name;
    private final List<Argument> args = new ArrayList<>();
    private final List<Block> blocks = new ArrayList<>();

    public VirtualFunction(Type type, String name) {
        super(type);
        this.name = name;
    }

    public boolean isDeclare() {
        return blocks.isEmpty();
    }

    public VirtualFunction addArg(Argument arg) {
        args.add(arg);
        return this;
    }

    public void addBlock(Block block) {
        blocks.add(block);
    }

    public void addBlock(int index, Block block) {
        blocks.add(index, block);
    }

    public void addBlocks(int index, Collection<Block> newBlocks) {
        blocks.addAll(index, newBlocks);
    }

    public void insertBlockAfter(Block base, Block block) {
        int index = blocks.indexOf(base);
        blocks.add(index + 1, block);
    }

    public List<Argument> getArgs() {
        return args;
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<Block> blocks) {
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
        for (Block block : blocks) {
            builder.append(block).append(":\n");
            for (VIR ir : block)
                builder.append("  ").append(ir).append('\n');
        }
        builder.append("}\n");
        return builder.toString();
    }
}
