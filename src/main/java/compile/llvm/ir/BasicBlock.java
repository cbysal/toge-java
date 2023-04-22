package compile.llvm.ir;

import compile.llvm.ir.instr.Instr;

public class BasicBlock {
    private static int autoTag;
    private final int id;
    private BasicBlock prev, next;
    private int instrNum;
    private Function parent;
    private Instr first, last;

    public BasicBlock(Function parent) {
        this.parent = parent;
        this.id = autoTag++;
    }

    public void addFirst(Instr instr) {
        if (instrNum == 0) {
            first = instr;
            last = instr;
            instrNum++;
            return;
        }
        instr.linkNext(first);
        first.linkPrev(instr);
        first = instr;
        instrNum++;
    }

    public void addLast(Instr instr) {
        if (instrNum == 0) {
            first = instr;
            last = instr;
            instrNum++;
            return;
        }
        last.linkNext(instr);
        instr.linkPrev(last);
        last = instr;
        instrNum++;
    }

    public Instr getFirst() {
        return first;
    }

    public void insertBefore(BasicBlock block) {
        parent.addBlockNum();
        if (prev == null) {
            parent.setFirst(block);
            block.next = this;
            prev = block;
            return;
        }
        BasicBlock oldPrev = prev;
        oldPrev.next = block;
        block.prev = oldPrev;
        block.next = this;
        prev = block;
    }

    public void insertAfter(BasicBlock block) {
        parent.addBlockNum();
        if (next == null) {
            parent.setLast(block);
            next = block;
            block.prev = this;
            return;
        }
        BasicBlock oldNext = next;
        next = block;
        block.prev = this;
        block.next = oldNext;
        oldNext.prev = block;
    }

    public void linkNext(BasicBlock block) {
        next = block;
    }

    public void linkPrev(BasicBlock block) {
        prev = block;
    }

    public BasicBlock getPrev() {
        return prev;
    }

    public BasicBlock getNext() {
        return next;
    }

    public void mergeFirst(BasicBlock newBlock) {
        if (newBlock.isEmpty()) {
            return;
        }
        Instr newFirst = newBlock.first;
        Instr newLast = newBlock.last;
        Instr oldFirst = first;
        first = newFirst;
        newLast.linkNext(oldFirst);
        oldFirst.linkPrev(newLast);
        instrNum += newBlock.instrNum;
    }

    public boolean isEmpty() {
        return instrNum == 0;
    }

    public String getTag() {
        return String.format("%%b%d", id);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append('b').append(id).append(":\n");
        for (Instr curInstr = first; curInstr != null; curInstr = curInstr.getNext()) {
            builder.append("  ").append(curInstr).append('\n');
        }
        return builder.toString();
    }
}
