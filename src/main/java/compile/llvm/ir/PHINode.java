package compile.llvm.ir;

import compile.llvm.BasicBlock;
import compile.llvm.type.Type;
import compile.llvm.value.Use;
import compile.llvm.value.Value;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class PHINode extends Instruction {
    private final Map<Use, BasicBlock> useBlockMap = new HashMap<>();

    public PHINode(BasicBlock block, Type type) {
        super(block, type);
    }

    public void add(BasicBlock block, Use use) {
        super.add(use);
        useBlockMap.put(use, block);
    }

    public Pair<BasicBlock, Value> getBlockValue(int index) {
        Use use = get(index);
        BasicBlock block = useBlockMap.get(use);
        return Pair.of(block, use.getValue());
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ");
        for (Use use : operands)
            joiner.add(String.format("[%s, %%%s]", use.getValue().getName(), useBlockMap.get(use)));
        return String.format("%s = phi %s %s", getName(), type, joiner);
    }
}
