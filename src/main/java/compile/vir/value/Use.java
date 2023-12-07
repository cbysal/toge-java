package compile.vir.value;

public class Use {
    private Value value;
    private Use prev, next;
    private User parent;

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
