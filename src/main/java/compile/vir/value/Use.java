package compile.vir.value;

public class Use {
    private final Value value;
    private final Use prev;
    private final Use next;
    private final User parent;

    public Use(Value value, Use prev, Use next, User parent) {
        this.value = value;
        this.prev = prev;
        this.next = next;
        this.parent = parent;
    }

    public Value getValue() {
        return value;
    }

    public Use getPrev() {
        return prev;
    }

    public Use getNext() {
        return next;
    }

    public User getParent() {
        return parent;
    }
}
