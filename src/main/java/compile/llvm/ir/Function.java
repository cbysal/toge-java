package compile.llvm.ir;

import compile.llvm.ir.type.BasicType;
import compile.llvm.ir.type.Type;

import java.util.ArrayList;
import java.util.List;

public class Function extends Value {
    private final List<Param> params = new ArrayList<>();
    private int blockNum;
    private BasicBlock first, last;

    public Function(Type type, String name) {
        super(type, name);
    }

    public void addParam(Param param) {
        params.add(param);
    }

    public List<Param> getParams() {
        return params;
    }

    public void addFirst(BasicBlock block) {
        if (blockNum == 0) {
            first = block;
            last = block;
            blockNum++;
            return;
        }
        block.linkNext(first);
        first.linkPrev(block);
        first = block;
        blockNum++;
    }

    public void addLast(BasicBlock block) {
        if (blockNum == 0) {
            first = block;
            last = block;
            blockNum++;
            return;
        }
        last.linkNext(block);
        block.linkPrev(last);
        last = block;
        blockNum++;
    }

    void setFirst(BasicBlock first) {
        this.first = first;
    }

    void setLast(BasicBlock last) {
        this.last = last;
    }

    void addBlockNum() {
        blockNum++;
    }

    @Override
    public BasicType getType() {
        return (BasicType) type; // safe, the return type is limited to int & float
    }

    public BasicBlock getFirst() {
        return first;
    }

    public BasicBlock getLast() {
        return last;
    }

    @Override
    public String getTag() {
        return "@" + name;
    }

    public String getDeclare() {
        StringBuilder builder = new StringBuilder();
        builder.append("declare ").append(getType()).append(" @").append(name).append('(');
        boolean isFirst = true;
        for (Param param : params) {
            if (!isFirst) {
                builder.append(", ");
            }
            builder.append(param.getRet());
            isFirst = false;
        }
        builder.append(")\n");
        return builder.toString();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("define dso_local ").append(getType()).append(" @").append(name).append('(');
        boolean isFirst = true;
        for (Param param : params) {
            if (!isFirst) {
                builder.append(", ");
            }
            builder.append(param.getRet());
            isFirst = false;
        }
        builder.append(") {\n");
        for (BasicBlock curBlock = first; curBlock != null; curBlock = curBlock.getNext()) {
            builder.append(curBlock);
        }
        builder.append("}\n");
        return builder.toString();
    }
}
